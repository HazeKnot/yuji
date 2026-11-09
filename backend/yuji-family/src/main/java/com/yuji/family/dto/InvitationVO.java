package com.yuji.family.dto;

import java.time.LocalDateTime;

/**
 * 邀请码。{@code hint} 直接给前端展示，避免前端硬编码规则文案。
 */
public record InvitationVO(String code, LocalDateTime expiresAt, String hint) {
}
