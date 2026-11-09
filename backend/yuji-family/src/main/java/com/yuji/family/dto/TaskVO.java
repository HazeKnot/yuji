package com.yuji.family.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务视图。{@code mine} 表示"这条是派给我的"，前端据此决定是否显示打卡按钮。
 */
public record TaskVO(
        Long id,
        String title,
        String detail,
        LocalDate dueDate,
        String status,
        Long assigneeUserId,
        String assigneeNickname,
        boolean mine,
        String templateCode,
        LocalDateTime completedAt,
        boolean overdue
) {
}
