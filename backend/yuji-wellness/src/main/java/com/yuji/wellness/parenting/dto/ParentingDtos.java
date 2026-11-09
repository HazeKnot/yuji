package com.yuji.wellness.parenting.dto;

import java.util.List;

public final class ParentingDtos {

    private ParentingDtos() {
    }

    public record CourseVO(
            Long id,
            String code,
            String title,
            String summary,
            Integer minMonth,
            Integer maxMonth,
            String monthHint,
            Integer lessonCount,
            String source
    ) {
    }

    public record LessonVO(
            Long id,
            String title,
            String content,
            Integer durationMin
    ) {
    }

    public record CourseDetailVO(
            CourseVO course,
            List<LessonVO> lessons,
            String disclaimer
    ) {
    }

    public record PageVO(
            Integer babyAgeMonths,
            String monthTip,
            List<CourseVO> courses,
            String disclaimer
    ) {
    }
}
