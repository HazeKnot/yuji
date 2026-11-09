package com.yuji.family.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskStatusReq(
        @NotBlank(message = "请指定任务状态")
        String status
) {
}
