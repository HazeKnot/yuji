package com.yuji.family.service;

import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.family.TableInfoInitializer;
import com.yuji.family.dto.FamilyVO;
import com.yuji.family.dto.InvitationVO;
import com.yuji.family.dto.MemberVO;
import com.yuji.family.dto.PermissionsReq;
import com.yuji.family.entity.Family;
import com.yuji.family.entity.FamilyInvitation;
import com.yuji.family.entity.FamilyMember;
import com.yuji.family.mapper.FamilyInvitationMapper;
import com.yuji.family.mapper.FamilyMapper;
import com.yuji.family.mapper.FamilyMemberMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 家庭绑定与字段级授权：W4 的安全边界几乎都集中在这一层，所以边界用例优先。
 */
@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    private static final Long MOM = 1L;
    private static final Long FAMILY_ID = 10L;
    private static final Long MEMBER_ID = 100L;

    @Mock
    private FamilyMapper familyMapper;
    @Mock
    private FamilyMemberMapper memberMapper;
    @Mock
    private FamilyInvitationMapper invitationMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FamilyService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoInitializer.init(Family.class, FamilyMember.class, FamilyInvitation.class);
    }

    // ---------- 邀请码 ----------

    @Test
    @DisplayName("首次生成邀请码：顺手创建家庭，码为 6 位且不含易混字符")
    void shouldCreateFamilyAndInvitation() {
        when(familyMapper.selectOne(any())).thenReturn(null);
        when(invitationMapper.selectCount(any())).thenReturn(0L);

        InvitationVO vo = service.createInvitation(MOM);

        ArgumentCaptor<Family> family = ArgumentCaptor.forClass(Family.class);
        verify(familyMapper).insert(family.capture());
        assertThat(family.getValue().getOwnerUserId()).isEqualTo(MOM);

        ArgumentCaptor<FamilyInvitation> inv = ArgumentCaptor.forClass(FamilyInvitation.class);
        verify(invitationMapper).insert(inv.capture());
        assertThat(inv.getValue().getCode()).hasSize(6).doesNotContain("0", "O", "1", "I");
        assertThat(inv.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
        assertThat(vo.code()).isEqualTo(inv.getValue().getCode());
    }

    @Test
    @DisplayName("已有家庭：不再重复创建")
    void shouldReuseExistingFamily() {
        Family existing = family(FAMILY_ID, MOM);
        when(familyMapper.selectOne(any())).thenReturn(existing);
        when(invitationMapper.selectCount(any())).thenReturn(0L);

        service.createInvitation(MOM);

        verify(familyMapper, never()).insert(any(Family.class));
        ArgumentCaptor<FamilyInvitation> inv = ArgumentCaptor.forClass(FamilyInvitation.class);
        verify(invitationMapper).insert(inv.capture());
        assertThat(inv.getValue().getFamilyId()).isEqualTo(FAMILY_ID);
    }

    // ---------- 加入 ----------

    @Test
    @DisplayName("家属加入：默认零授权（只见任务与状态摘要）")
    void shouldJoinWithNoPermissions() {
        FamilyInvitation inv = invitation(FAMILY_ID, null, LocalDateTime.now().plusHours(1));
        when(invitationMapper.selectOne(any())).thenReturn(inv);
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(invitationMapper.update(isNull(), any())).thenReturn(1);
        when(memberMapper.selectCount(any())).thenReturn(0L);

        FamilyVO vo = service.join(9L, " abc123 ");

        assertThat(vo.role()).isEqualTo("MEMBER");
        ArgumentCaptor<FamilyMember> saved = ArgumentCaptor.forClass(FamilyMember.class);
        verify(memberMapper).insert(saved.capture());
        assertThat(saved.getValue().getVisibleFields()).isEmpty();
        assertThat(saved.getValue().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("邀请码不存在 / 已用 / 过期：都返回 40000，且不落成员")
    void shouldRejectInvalidInvitation() {
        when(invitationMapper.selectOne(any())).thenReturn(null);
        assert40000(() -> service.join(9L, "NOPE00"));

        FamilyInvitation used = invitation(FAMILY_ID, 8L, LocalDateTime.now().plusHours(1));
        when(invitationMapper.selectOne(any())).thenReturn(used);
        assertThatThrownBy(() -> service.join(9L, "ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被使用");

        FamilyInvitation expired = invitation(FAMILY_ID, null, LocalDateTime.now().minusMinutes(1));
        when(invitationMapper.selectOne(any())).thenReturn(expired);
        assertThatThrownBy(() -> service.join(9L, "ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");

        verify(memberMapper, never()).insert(any(FamilyMember.class));
    }

    @Test
    @DisplayName("不能加入自己创建的家庭")
    void shouldRejectJoiningOwnFamily() {
        when(invitationMapper.selectOne(any()))
                .thenReturn(invitation(FAMILY_ID, null, LocalDateTime.now().plusHours(1)));
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(family(FAMILY_ID, MOM));

        assertThatThrownBy(() -> service.join(MOM, "ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己创建");
        verify(memberMapper, never()).insert(any(FamilyMember.class));
    }

    @Test
    @DisplayName("并发抢同一个码：条件更新失败的人被拒（防重复入组）")
    void shouldRejectWhenCodeClaimedConcurrently() {
        when(invitationMapper.selectOne(any()))
                .thenReturn(invitation(FAMILY_ID, null, LocalDateTime.now().plusHours(1)));
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(invitationMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.join(9L, "ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("刚被使用");
        verify(memberMapper, never()).insert(any(FamilyMember.class));
    }

    @Test
    @DisplayName("已在别的家庭：40900")
    void shouldRejectSecondFamily() {
        when(invitationMapper.selectOne(any()))
                .thenReturn(invitation(FAMILY_ID, null, LocalDateTime.now().plusHours(1)));
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectOne(any())).thenReturn(member(FAMILY_ID, 9L, ""));

        assertThatThrownBy(() -> service.join(9L, "ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.CONFLICT.getCode());
    }

    // ---------- 授权 ----------

    @Test
    @DisplayName("非妈妈改授权：40300（越权防线在服务层，不靠前端隐藏按钮）")
    void shouldRejectPermissionUpdateByNonOwner() {
        when(familyMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.updatePermissions(9L, MEMBER_ID, new PermissionsReq(List.of("WEIGHT"), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("授权字段只接受指标四类：传情绪类字段直接 40000")
    void shouldRejectNonMetricField() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member(FAMILY_ID, 9L, ""));

        assertThatThrownBy(() -> service.updatePermissions(MOM, MEMBER_ID,
                new PermissionsReq(List.of("WEIGHT", "MOOD_LOG"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能授权指标类字段");
        verify(memberMapper, never()).updateById(any(FamilyMember.class));
    }

    @Test
    @DisplayName("授权正常：大小写归一 + 去重 + 逗号落库")
    void shouldNormalizePermissions() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member(FAMILY_ID, 9L, ""));
        when(userMapper.selectById(9L)).thenReturn(user(9L, "阿哲"));

        MemberVO vo = service.updatePermissions(MOM, MEMBER_ID,
                new PermissionsReq(List.of("weight", "WEIGHT", "BLOOD_PRESSURE"), "配偶"));

        ArgumentCaptor<FamilyMember> updated = ArgumentCaptor.forClass(FamilyMember.class);
        verify(memberMapper).updateById(updated.capture());
        assertThat(updated.getValue().getVisibleFields()).isEqualTo("WEIGHT,BLOOD_PRESSURE");
        assertThat(updated.getValue().getRelation()).isEqualTo("配偶");
        assertThat(vo.visibleFields()).containsExactly("WEIGHT", "BLOOD_PRESSURE");
        assertThat(vo.nickname()).isEqualTo("阿哲");
    }

    @Test
    @DisplayName("移除成员：只能移除自己家庭的成员（否则 40400）")
    void shouldRejectRemovingForeignMember() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member(999L, 9L, ""));

        assertThatThrownBy(() -> service.removeMember(MOM, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        verify(memberMapper, never()).deleteById(any(java.io.Serializable.class));
    }

    // ---------- 归属 ----------

    @Test
    @DisplayName("mine：三种角色（未加入 / 妈妈 / 家属）")
    void shouldResolveRole() {
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(familyMapper.selectOne(any())).thenReturn(null);
        assertThat(service.mine(9L).role()).isEqualTo("NONE");

        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectCount(any())).thenReturn(0L);
        assertThat(service.mine(MOM).role()).isEqualTo("OWNER");

        when(memberMapper.selectOne(any())).thenReturn(member(FAMILY_ID, 9L, "WEIGHT"));
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(family(FAMILY_ID, MOM));
        FamilyVO memberView = service.mine(9L);
        assertThat(memberView.role()).isEqualTo("MEMBER");
        assertThat(memberView.ownerUserId()).isEqualTo(MOM);
    }

    @Test
    @DisplayName("可授权字段清单 = 指标四类，不含情绪/量表")
    void shouldExposeOnlyMetricFields() {
        assertThat(service.shareFields())
                .extracting(f -> f.code())
                .containsExactly("WEIGHT", "BLOOD_PRESSURE", "BLOOD_GLUCOSE", "FETAL_MOVEMENT")
                .doesNotContain("MOOD_LOG", "EPDS");
    }

    @Test
    @DisplayName("未加入家庭就访问家庭内接口：40300")
    void shouldRejectNonMember() {
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(familyMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.requireFamily(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("空邀请码：连库都不查，直接 40000")
    void shouldRejectBlankCode() {
        assert40000(() -> service.join(9L, "   "));
        verify(invitationMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("邀请码指向的家庭已被删：40400")
    void shouldRejectMissingFamilyOnJoin() {
        when(invitationMapper.selectOne(any()))
                .thenReturn(invitation(FAMILY_ID, null, LocalDateTime.now().plusHours(1)));
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.join(9L, "ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("成员表指向不存在的家庭（历史脏数据）：mine 退化到 NONE，不抛异常")
    void shouldFallbackToNoneOnDirtyMemberRow() {
        when(memberMapper.selectOne(any())).thenReturn(member(FAMILY_ID, 9L, ""));
        when(familyMapper.selectById(FAMILY_ID)).thenReturn(null);
        when(familyMapper.selectOne(any())).thenReturn(null);

        assertThat(service.mine(9L).role()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("没有成员时列表为空，不查用户表")
    void shouldReturnEmptyMembers() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectList(any())).thenReturn(List.of());

        assertThat(service.members(MOM)).isEmpty();
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    @DisplayName("用户已注销：昵称回退为「家人」而不是 null")
    void shouldFallbackNicknameWhenUserGone() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectList(any())).thenReturn(List.of(member(FAMILY_ID, 9L, "")));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        assertThat(service.members(MOM).get(0).nickname()).isEqualTo("家人");
    }

    @Test
    @DisplayName("清空授权：传空白串等于全部收回（只保留任务与摘要）")
    void shouldClearAllPermissions() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member(FAMILY_ID, 9L, "WEIGHT"));
        when(userMapper.selectById(9L)).thenReturn(user(9L, "阿哲"));

        MemberVO vo = service.updatePermissions(MOM, MEMBER_ID,
                new PermissionsReq(List.of("  ", ""), "   "));

        assertThat(vo.visibleFields()).isEmpty();
        assertThat(vo.relation()).isEqualTo("家人");
        ArgumentCaptor<FamilyMember> updated = ArgumentCaptor.forClass(FamilyMember.class);
        verify(memberMapper).updateById(updated.capture());
        assertThat(updated.getValue().getVisibleFields()).isEmpty();
        assertThat(updated.getValue().getRelation()).isNull();
    }

    @Test
    @DisplayName("移除成员：成功路径落库删除")
    void shouldRemoveMember() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member(FAMILY_ID, 9L, ""));

        service.removeMember(MOM, MEMBER_ID);

        verify(memberMapper).deleteById(MEMBER_ID);
    }

    @Test
    @DisplayName("邀请码连续 5 次冲突：放弃并报业务错误（不无限重试）")
    void shouldGiveUpAfterCodeRetries() {
        when(familyMapper.selectOne(any())).thenReturn(family(FAMILY_ID, MOM));
        when(invitationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createInvitation(MOM))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.BUSINESS_ERROR.getCode());
        verify(invitationMapper, never()).insert(any(FamilyInvitation.class));
    }

    @Test
    @DisplayName("splitFields：null / 空白 / 带空格都安全")
    void shouldSplitFieldsSafely() {
        assertThat(FamilyService.splitFields(null)).isEmpty();
        assertThat(FamilyService.splitFields("   ")).isEmpty();
        assertThat(FamilyService.splitFields("WEIGHT, BLOOD_PRESSURE"))
                .containsExactly("WEIGHT", "BLOOD_PRESSURE");
    }

    @Test
    @DisplayName("nicknameOf(null) 返回空串（不 NPE）")
    void shouldHandleNullNickname() {
        assertThat(service.nicknameOf(null)).isEmpty();
    }

    // ---------- 夹具 ----------

    private void assert40000(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.PARAM_ERROR.getCode());
    }

    private static Family family(Long id, Long owner) {
        Family f = new Family();
        f.setId(id);
        f.setOwnerUserId(owner);
        return f;
    }

    private static FamilyMember member(Long familyId, Long userId, String fields) {
        FamilyMember m = new FamilyMember();
        m.setId(MEMBER_ID);
        m.setFamilyId(familyId);
        m.setUserId(userId);
        m.setRelation("家人");
        m.setVisibleFields(fields);
        m.setStatus("ACTIVE");
        return m;
    }

    private static FamilyInvitation invitation(Long familyId, Long usedBy, LocalDateTime expiresAt) {
        FamilyInvitation inv = new FamilyInvitation();
        inv.setId(1L);
        inv.setCode("ABCDEF");
        inv.setFamilyId(familyId);
        inv.setUsedBy(usedBy);
        inv.setExpiresAt(expiresAt);
        return inv;
    }

    private static User user(Long id, String nickname) {
        User u = new User();
        u.setId(id);
        u.setNickname(nickname);
        return u;
    }
}
