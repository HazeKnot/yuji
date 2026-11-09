package com.yuji.profile.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标记录返回体。type 是机读码，typeLabel/unit 是给前端直接展示的文案。
 */
public record HealthRecordVO(
        Long id,
        String type,
        String typeLabel,
        BigDecimal value,
        BigDecimal value2,
        String unit,
        LocalDateTime measuredAt,
        String note
) {
}
