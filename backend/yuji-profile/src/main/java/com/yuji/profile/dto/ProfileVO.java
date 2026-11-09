package com.yuji.profile.dto;

import java.time.LocalDate;
import java.util.List;

public record ProfileVO(
        Long id,
        String nickname,
        LocalDate lmpDate,
        LocalDate edd,
        List<String> concerns,
        Integer weeks,
        Integer days,
        String stage,
        Long daysToEdd,
        Boolean postpartum
) {
}
