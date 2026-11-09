package com.yuji.wellness.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.wellness.community.entity.CommunityPost;
import com.yuji.wellness.community.mapper.CommunityPostMapper;
import com.yuji.wellness.cycle.mapper.CycleRecordMapper;
import com.yuji.wellness.mental.dto.MentalDtos;
import com.yuji.wellness.mental.entity.EmotionCheckin;
import com.yuji.wellness.mental.entity.RiskTicket;
import com.yuji.wellness.mental.entity.ScaleResult;
import com.yuji.wellness.mental.mapper.EmotionCheckinMapper;
import com.yuji.wellness.mental.mapper.RiskTicketMapper;
import com.yuji.wellness.mental.mapper.ScaleResultMapper;
import com.yuji.wellness.mental.service.MentalService;
import com.yuji.wellness.partner.dto.PartnerDtos;
import com.yuji.wellness.partner.entity.PartnerLead;
import com.yuji.wellness.partner.entity.PartnerOrg;
import com.yuji.wellness.partner.mapper.PartnerLeadMapper;
import com.yuji.wellness.partner.mapper.PartnerOrgMapper;
import com.yuji.wellness.support.RiskLevel;
import com.yuji.wellness.support.TimeText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 机构端服务：总览聚合 + 会员管理 + AI 风控告警。
 *
 * <p>边界不变：不下发明细健康数据，只给基础资料、线索跟进与风险摘要。
 * orgId 为 null（ADMIN）时是全平台视图；ORG 账号的数据严格按租户隔离。
 */
@Service
@RequiredArgsConstructor
public class AdminWellnessService {

    private final UserMapper userMapper;
    private final CycleRecordMapper cycleMapper;
    private final CommunityPostMapper postMapper;
    private final RiskTicketMapper ticketMapper;
    private final PartnerLeadMapper leadMapper;
    private final PartnerOrgMapper orgMapper;
    private final EmotionCheckinMapper checkinMapper;
    private final ScaleResultMapper scaleMapper;
    private final MentalService mentalService;

    public PartnerDtos.AdminOverviewVO overview() {
        return new PartnerDtos.AdminOverviewVO(
                userMapper.selectCount(null),
                cycleMapper.selectCount(null),
                postMapper.selectCount(new LambdaQueryWrapper<CommunityPost>().isNull(CommunityPost::getDeletedAt)),
                postMapper.selectCount(new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getStatus, "PENDING")
                        .isNull(CommunityPost::getDeletedAt)),
                ticketMapper.selectCount(new LambdaQueryWrapper<RiskTicket>().ne(RiskTicket::getStatus, "CLOSED")),
                ticketMapper.selectCount(new LambdaQueryWrapper<RiskTicket>()
                        .eq(RiskTicket::getLevel, "RED")
                        .ne(RiskTicket::getStatus, "CLOSED")),
                leadMapper.selectCount(null),
                leadMapper.selectCount(new LambdaQueryWrapper<PartnerLead>().eq(PartnerLead::getStatus, "NEW")),
                orgMapper.selectCount(new LambdaQueryWrapper<PartnerOrg>()
                        .eq(PartnerOrg::getStatus, "ACTIVE")
                        .isNull(PartnerOrg::getDeletedAt)),
                checkinMapper.selectCount(null),
                scaleMapper.selectCount(null)
        );
    }

    // ---------- 会员管理（SaaS 租户核心） ----------

    /** 本机构会员列表：提交过预约意向并绑定平台账号的用户，附最新风险摘要 */
    public List<PartnerDtos.MemberVO> members(Long orgId, String keyword) {
        List<PartnerLead> leads = leadMapper.selectList(new LambdaQueryWrapper<PartnerLead>()
                .eq(PartnerLead::getOrgId, orgId)
                .isNotNull(PartnerLead::getUserId)
                .orderByDesc(PartnerLead::getCreatedAt));
        if (leads.isEmpty()) {
            return List.of();
        }
        Map<Long, List<PartnerLead>> byUser = leads.stream()
                .filter(l -> l.getUserId() != null)
                .collect(Collectors.groupingBy(PartnerLead::getUserId));

        Set<Long> userIds = byUser.keySet();
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, List<RiskTicket>> ticketsByUser = ticketMapper.selectList(
                        new LambdaQueryWrapper<RiskTicket>()
                                .in(RiskTicket::getUserId, userIds)
                                .orderByDesc(RiskTicket::getCreatedAt))
                .stream().collect(Collectors.groupingBy(RiskTicket::getUserId));

        return userIds.stream()
                .map(uid -> buildMemberVO(uid, byUser.get(uid), users.get(uid), ticketsByUser.get(uid)))
                .filter(Objects::nonNull)
                .filter(vo -> keyword == null || keyword.isBlank()
                        || (vo.nickname() != null && vo.nickname().contains(keyword.trim()))
                        || (vo.phone() != null && vo.phone().contains(keyword.trim())))
                .sorted(Comparator.comparing(PartnerDtos.MemberVO::joinedAt).reversed())
                .toList();
    }

    /** 会员详情：基础资料 + 本机构线索 + 风险摘要工单（不下发健康明细） */
    public PartnerDtos.MemberDetailVO memberDetail(Long orgId, Long userId) {
        List<PartnerLead> leads = leadMapper.selectList(new LambdaQueryWrapper<PartnerLead>()
                .eq(PartnerLead::getOrgId, orgId)
                .eq(PartnerLead::getUserId, userId)
                .orderByDesc(PartnerLead::getCreatedAt));
        if (leads.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该用户不属于本机构");
        }
        User user = userMapper.selectById(userId);
        List<RiskTicket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<RiskTicket>()
                .eq(RiskTicket::getUserId, userId)
                .orderByDesc(RiskTicket::getCreatedAt)
                .last("LIMIT 20"));
        PartnerDtos.MemberVO member = buildMemberVO(userId, leads, user, tickets);
        List<PartnerDtos.LeadVO> leadVOs = leads.stream().map(l -> new PartnerDtos.LeadVO(
                l.getId(), l.getOrgId(), orgName(l.getOrgId()), l.getName(), l.getPhone(),
                l.getNeed(), l.getPreferredDate(), l.getStatus(), leadStatusLabel(l.getStatus()),
                l.getNote(), TimeText.of(l.getCreatedAt()), l.getCreatedAt())).toList();
        List<MentalDtos.TicketVO> ticketVOs = tickets.stream().map(this::toTicketVO).toList();
        return new PartnerDtos.MemberDetailVO(member, leadVOs, ticketVOs);
    }

    // ---------- AI 风控告警 ----------

    /** AI 风控告警：社区帖子 / 情绪打卡触发的工单（orgId 为 null 时是全平台） */
    public List<MentalDtos.TicketVO> alerts(Long orgId, String status, String level) {
        Set<String> sources = Set.of("COMMUNITY", "EMOTION");
        if (orgId == null) {
            return mentalService.adminTickets(status, level, null).stream()
                    .filter(t -> sources.contains(t.source()))
                    .toList();
        }
        Set<Long> memberIds = leadMapper.selectList(new LambdaQueryWrapper<PartnerLead>()
                        .eq(PartnerLead::getOrgId, orgId)
                        .isNotNull(PartnerLead::getUserId))
                .stream().map(PartnerLead::getUserId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return mentalService.adminTickets(status, level, memberIds).stream()
                .filter(t -> sources.contains(t.source()))
                .toList();
    }

    /** 工单跟进状态更新（转给 MentalService 保持同一套校验） */
    public MentalDtos.TicketVO updateTicket(Long id, PartnerDtos.TicketStatusReq req) {
        return mentalService.updateTicket(id, req);
    }

    // ---------- 内部 ----------

    private PartnerDtos.MemberVO buildMemberVO(Long userId, List<PartnerLead> leads,
                                               User user, List<RiskTicket> tickets) {
        if (leads == null || leads.isEmpty()) {
            return null;
        }
        PartnerLead latest = leads.get(0);
        PartnerLead earliest = leads.stream()
                .min(Comparator.comparing(PartnerLead::getCreatedAt)).orElse(latest);
        RiskTicket lastTicket = tickets == null ? null : tickets.get(0);
        RiskLevel lastLevel = lastTicket == null ? null : RiskLevel.of(lastTicket.getLevel());
        return new PartnerDtos.MemberVO(
                userId,
                user == null ? "用户" + userId : user.getNickname(),
                user == null ? latest.getPhone() : user.getPhone(),
                leads.size(),
                latest.getStatus(), leadStatusLabel(latest.getStatus()),
                lastTicket == null ? null : lastTicket.getLevel(),
                lastLevel == null ? null : lastLevel.label(),
                lastTicket == null ? null : lastTicket.getSummary(),
                TimeText.of(earliest.getCreatedAt()),
                earliest.getCreatedAt());
    }

    private MentalDtos.TicketVO toTicketVO(RiskTicket row) {
        RiskLevel level = RiskLevel.of(row.getLevel());
        return new MentalDtos.TicketVO(row.getId(), row.getSource(), level.name(), level.label(),
                row.getSummary(), row.getStatus(), MentalService.statusLabel(row.getStatus()),
                row.getHandlerNote(), TimeText.of(row.getCreatedAt()), row.getCreatedAt());
    }

    private String orgName(Long orgId) {
        PartnerOrg org = orgId == null ? null : orgMapper.selectById(orgId);
        return org == null ? "" : org.getName();
    }

    private static String leadStatusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "NEW" -> "待跟进";
            case "CONTACTED" -> "已联系";
            case "BOOKED" -> "已预约";
            case "CLOSED" -> "已关闭";
            default -> status == null ? "" : status;
        };
    }
}
