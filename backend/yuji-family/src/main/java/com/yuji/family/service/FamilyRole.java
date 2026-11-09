package com.yuji.family.service;

/**
 * 当前用户在某家庭中的角色。
 *
 * <p>特意不复用 {@code user.role}：JWT 里的角色是签发时快照，加入家庭后改库会导致
 * 旧 token 与新角色不一致；家庭权限一律实时查 {@code family_member} 表。
 */
public enum FamilyRole {

    /** 未加入任何家庭 */
    NONE,
    /** 妈妈（建档主体） */
    OWNER,
    /** 家属 */
    MEMBER
}
