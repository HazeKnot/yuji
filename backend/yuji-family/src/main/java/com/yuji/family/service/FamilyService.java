package com.yuji.family.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.family.dto.FamilyVO;
import com.yuji.family.dto.InvitationVO;
import com.yuji.family.dto.MemberVO;
import com.yuji.family.dto.PermissionsReq;
import com.yuji.family.dto.ShareFieldVO;
import com.yuji.family.entity.Family;
import com.yuji.family.entity.FamilyInvitation;
import com.yuji.family.entity.FamilyMember;
import com.yuji.family.mapper.FamilyInvitationMapper;
import com.yuji.family.mapper.FamilyMapper;
import com.yuji.family.mapper.FamilyMemberMapper;
import com.yuji.profile.service.MetricType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 家庭与成员权限（模块 C 的核心约束都集中在这里）。
 *
 * <p>三条硬规则：
 * <ol>
 *   <li>邀请码一次性 + 24 小时过期，用条件更新抢占，防并发重复入组</li>
 *   <li>只有 OWNER（妈妈）能生成邀请码、改授权、移除成员、下发任务</li>
 *   <li>可授权字段只有指标四类；情绪/量表不在白名单，家属永远不可见</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    /** 去掉了容易看错的 0/O/1/I */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final Duration TTL = Duration.ofHours(24);
    private static final int MAX_CODE_RETRY = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper memberMapper;
    private final FamilyInvitationMapper invitationMapper;
    private final UserMapper userMapper;

    // ---------- 邀请码 ----------

    /** 妈妈生成邀请码：没有家庭时顺手创建（少一步操作，绑定 ≤ 1 分钟） */
    @Transactional
    public InvitationVO createInvitation(Long momUserId) {
        Family family = findByOwner(momUserId);
        if (family == null) {
            family = new Family();
            family.setOwnerUserId(momUserId);
            family.setName("我的家");
            familyMapper.insert(family);
        }

        String code = null;
        for (int i = 0; i < MAX_CODE_RETRY && code == null; i++) {
            String candidate = randomCode();
            Long exists = invitationMapper.selectCount(
                    new LambdaQueryWrapper<FamilyInvitation>().eq(FamilyInvitation::getCode, candidate));
            if (exists == null || exists == 0) {
                code = candidate;
            }
        }
        if (code == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "邀请码生成失败，请重试");
        }

        FamilyInvitation inv = new FamilyInvitation();
        inv.setCode(code);
        inv.setFamilyId(family.getId());
        inv.setCreatedBy(momUserId);
        inv.setExpiresAt(LocalDateTime.now().plus(TTL));
        invitationMapper.insert(inv);
        return new InvitationVO(code, inv.getExpiresAt(), "24 小时内有效，且只能使用一次");
    }

    /** 家属凭码加入：抢占式更新邀请码，避免两人同时用同一码 */
    @Transactional
    public FamilyVO join(Long joinerUserId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入邀请码");
        }
        FamilyInvitation inv = invitationMapper.selectOne(
                new LambdaQueryWrapper<FamilyInvitation>().eq(FamilyInvitation::getCode, code));
        if (inv == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码不存在，请核对后重试");
        }
        if (inv.getUsedBy() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码已被使用，请让家人重新生成");
        }
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邀请码已过期，请让家人重新生成");
        }
        Family family = familyMapper.selectById(inv.getFamilyId());
        if (family == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "家庭不存在");
        }
        if (family.getOwnerUserId().equals(joinerUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能加入自己创建的家庭");
        }
        if (findMember(joinerUserId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经加入了一个家庭");
        }

        int claimed = invitationMapper.update(null, new LambdaUpdateWrapper<FamilyInvitation>()
                .eq(FamilyInvitation::getId, inv.getId())
                .isNull(FamilyInvitation::getUsedBy)
                .set(FamilyInvitation::getUsedBy, joinerUserId)
                .set(FamilyInvitation::getUsedAt, LocalDateTime.now()));
        if (claimed == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邀请码刚被使用，请让家人重新生成");
        }

        FamilyMember m = new FamilyMember();
        m.setFamilyId(family.getId());
        m.setUserId(joinerUserId);
        m.setRelation("家人");
        m.setVisibleFields("");
        m.setStatus("ACTIVE");
        memberMapper.insert(m);
        return toFamilyVO(family, FamilyRole.MEMBER);
    }

    // ---------- 查询 ----------

    public FamilyVO mine(Long userId) {
        FamilyMember m = findMember(userId);
        if (m != null) {
            Family family = familyMapper.selectById(m.getFamilyId());
            if (family != null) {
                return toFamilyVO(family, FamilyRole.MEMBER);
            }
        }
        Family owned = findByOwner(userId);
        return owned == null ? FamilyVO.none() : toFamilyVO(owned, FamilyRole.OWNER);
    }

    public List<MemberVO> members(Long momUserId) {
        Family family = requireOwnedFamily(momUserId);
        return memberVOs(family.getId());
    }

    public List<ShareFieldVO> shareFields() {
        return Arrays.stream(MetricType.values())
                .map(t -> new ShareFieldVO(t.name(), t.label(), t.unit()))
                .toList();
    }

    // ---------- 授权 ----------

    /** 只允许指标类字段；情绪/量表即使传进来也会被拒（并回显允许清单） */
    @Transactional
    public MemberVO updatePermissions(Long momUserId, Long memberId, PermissionsReq req) {
        Family family = requireOwnedFamily(momUserId);
        FamilyMember m = requireMemberOf(family.getId(), memberId);

        Set<String> allowed = Arrays.stream(MetricType.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> picked = new LinkedHashSet<>();
        if (req.visibleFields() != null) {
            for (String raw : req.visibleFields()) {
                String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
                if (code.isEmpty()) {
                    continue;
                }
                if (!allowed.contains(code)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "只能授权指标类字段：" + String.join(" / ", allowed));
                }
                picked.add(code);
            }
        }

        String relation = req.relation() != null && !req.relation().isBlank()
                ? req.relation().trim() : m.getRelation();

        FamilyMember update = new FamilyMember();
        update.setId(m.getId());
        update.setVisibleFields(String.join(",", picked));
        if (!relation.equals(m.getRelation())) {
            update.setRelation(relation);
        }
        memberMapper.updateById(update);

        // 直接用已知值回填，避免再查一次库（也避免读到自己刚写的旧快照）
        return new MemberVO(m.getId(), m.getUserId(), nicknameOf(m.getUserId()),
                relation, List.copyOf(picked), m.getJoinedAt());
    }

    @Transactional
    public void removeMember(Long momUserId, Long memberId) {
        Family family = requireOwnedFamily(momUserId);
        FamilyMember m = requireMemberOf(family.getId(), memberId);
        memberMapper.deleteById(m.getId());
    }

    // ---------- 供任务/共享视图复用 ----------

    /** 家庭归属（含角色），未加入任何家庭时 40300 */
    public Membership requireFamily(Long userId) {
        FamilyMember m = findMember(userId);
        if (m != null) {
            Family family = familyMapper.selectById(m.getFamilyId());
            if (family != null) {
                return new Membership(family, FamilyRole.MEMBER, m);
            }
        }
        Family owned = findByOwner(userId);
        if (owned != null) {
            return new Membership(owned, FamilyRole.OWNER, null);
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "还没有家庭，请先邀请家人或输入邀请码加入");
    }

    public Family requireOwnedFamily(Long userId) {
        Family family = findByOwner(userId);
        if (family == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有妈妈可以执行此操作");
        }
        return family;
    }

    public FamilyMember findMember(Long userId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userId)
                .eq(FamilyMember::getStatus, "ACTIVE"));
    }

    public Family findByOwner(Long userId) {
        return familyMapper.selectOne(new LambdaQueryWrapper<Family>()
                .eq(Family::getOwnerUserId, userId));
    }

    public String nicknameOf(Long userId) {
        if (userId == null) {
            return "";
        }
        User u = userMapper.selectById(userId);
        return u == null || u.getNickname() == null ? "" : u.getNickname();
    }

    public List<MemberVO> memberVOs(Long familyId) {
        List<FamilyMember> list = memberMapper.selectList(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getStatus, "ACTIVE")
                .orderByAsc(FamilyMember::getId));
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(
                        list.stream().map(FamilyMember::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream()
                .map(m -> toMemberVO(m, users.containsKey(m.getUserId())
                        ? users.get(m.getUserId()).getNickname() : ""))
                .toList();
    }

    // ---------- 内部 ----------

    private FamilyMember requireMemberOf(Long familyId, Long memberId) {
        FamilyMember m = memberMapper.selectById(memberId);
        if (m == null || !m.getFamilyId().equals(familyId) || !"ACTIVE".equals(m.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "成员不存在");
        }
        return m;
    }

    private FamilyVO toFamilyVO(Family family, FamilyRole role) {
        int count = Math.toIntExact(memberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, family.getId())
                .eq(FamilyMember::getStatus, "ACTIVE")));
        return new FamilyVO(family.getId(), role.name(), family.getOwnerUserId(),
                nicknameOf(family.getOwnerUserId()), count);
    }

    private MemberVO toMemberVO(FamilyMember m, String nickname) {
        return new MemberVO(m.getId(), m.getUserId(),
                nickname == null || nickname.isBlank() ? "家人" : nickname,
                m.getRelation(), splitFields(m.getVisibleFields()), m.getJoinedAt());
    }

    static List<String> splitFields(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            if (!s.isBlank()) {
                out.add(s.trim());
            }
        }
        return out;
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 家庭 + 我在其中的角色（MEMBER 时带成员记录） */
    public record Membership(Family family, FamilyRole role, FamilyMember member) {

        public boolean isOwner() {
            return role == FamilyRole.OWNER;
        }

        public Long ownerUserId() {
            return family.getOwnerUserId();
        }
    }
}
