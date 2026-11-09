package com.yuji.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 下发任务包：按模板一次性生成多条任务给某个成员。
 */
public record TaskAssignReq(
        @NotBlank(message = "请选择任务包")
        String templateCode,

        @NotNull(message = "请选择执行人")
        Long assigneeUserId
) {
}
