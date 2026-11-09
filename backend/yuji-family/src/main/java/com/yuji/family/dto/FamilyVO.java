package com.yuji.family.dto;

/**
 * 当前用户的家庭归属。{@code role} 取 NONE / OWNER / MEMBER，前端据此切换视图。
 */
public record FamilyVO(
        Long familyId,
        String role,
        Long ownerUserId,
        String ownerNickname,
        int memberCount
) {

    public static FamilyVO none() {
        return new FamilyVO(null, "NONE", null, null, 0);
    }
}
