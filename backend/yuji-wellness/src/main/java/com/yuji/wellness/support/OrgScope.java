package com.yuji.wellness.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.auth.entity.User;
import com.yuji.auth.mapper.UserMapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;
import com.yuji.wellness.partner.entity.PartnerLead;
import com.yuji.wellness.partner.mapper.PartnerLeadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 机构租户边界：ORG 账号必须绑定 partner_org，数据按 org_id 隔离。
 *
 * <p>机构"自己的用户" = 向该机构提交过预约意向的用户（partner_lead.user_id 去重）。
 * ADMIN 角色视为平台运营，返回 null 表示不隔离（全平台视图）。
 */
@Component
@RequiredArgsConstructor
public class OrgScope {

    private final UserMapper userMapper;
    private final PartnerLeadMapper leadMapper;

    /**
     * @return 当前机构账号绑定的 org_id；ADMIN 返回 null（全平台）；未绑定的 ORG 直接 403
     */
    public Long resolve(LoginUser me) {
        AdminGuard.requireOrg(me);
        if (Roles.ADMIN.equals(me.role())) {
            return null;
        }
        User user = userMapper.selectById(me.userId());
        Long orgId = user == null ? null : user.getOrgId();
        if (orgId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号未绑定机构，请联系平台运营开通");
        }
        return orgId;
    }

    /** 本机构的会员 user_id 集合：提交过预约意向并关联了平台账号的用户 */
    public Set<Long> memberIds(Long orgId) {
        return leadMapper.selectList(new LambdaQueryWrapper<PartnerLead>()
                        .eq(PartnerLead::getOrgId, orgId)
                        .isNotNull(PartnerLead::getUserId))
                .stream().map(PartnerLead::getUserId).collect(Collectors.toSet());
    }
}
