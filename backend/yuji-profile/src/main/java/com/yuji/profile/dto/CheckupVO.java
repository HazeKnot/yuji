package com.yuji.profile.dto;

import java.time.LocalDate;

public record CheckupVO(
        Long id,
        String code,
        String name,
        Integer suggestedWeek,
        String windowText,
        LocalDate dueDate,
        String status,
        boolean overdue,
        String description
) {

    public static final String PLANNED = "PLANNED";
    public static final String DONE = "DONE";
    public static final String SKIPPED = "SKIPPED";
}
