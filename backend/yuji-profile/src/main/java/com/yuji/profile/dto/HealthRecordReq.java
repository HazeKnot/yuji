package com.yuji.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标录入请求。数值范围由 {@code MetricType} 在服务层做硬校验（不同指标范围不同）。
 */
public record HealthRecordReq(
        @NotBlank(message = "请指定指标类型")
        String type,

        @NotNull(message = "请填写指标数值")
        BigDecimal value,

        /** 仅血压使用：舒张压 */
        BigDecimal value2,

        /** 为空表示"现在" */
        LocalDateTime measuredAt,

        @Size(max = 200, message = "备注不能超过 200 字")
        String note
) {
}
