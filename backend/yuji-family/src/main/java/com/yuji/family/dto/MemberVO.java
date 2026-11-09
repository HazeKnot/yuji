package com.yuji.family.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 家庭成员 + 已授权字段。{@code visibleFields} 为空表示只见任务与状态摘要。
 */
public record MemberVO(
        Long id,
        Long userId,
        String nickname,
        String relation,
        List<String> visibleFields,
        LocalDateTime joinedAt
) {
}
