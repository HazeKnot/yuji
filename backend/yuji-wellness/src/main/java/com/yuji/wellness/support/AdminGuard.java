package com.yuji.wellness.support;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;

/**
 * 机构端接口的角色闸：与 AdminDashboardService 同一套判断，避免各写一遍。
 */
public final class AdminGuard {

    private AdminGuard() {
    }

    public static void requireOrg(LoginUser me) {
        if (me == null || (!Roles.ORG.equals(me.role()) && !Roles.ADMIN.equals(me.role()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该接口仅限机构账号访问");
        }
    }
}
