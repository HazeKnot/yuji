package com.yuji.common.security;

/**
 * 认证后的当前用户（作为 Spring Security principal）。
 * 角色常量见 {@link Roles}。
 */
public record LoginUser(Long userId, String role) {

    public boolean isMom() {
        return Roles.MOM.equals(role);
    }
}
