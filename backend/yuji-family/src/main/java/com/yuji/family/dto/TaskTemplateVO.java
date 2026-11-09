package com.yuji.family.dto;

import java.util.List;

/**
 * 任务包模板：{@code items} 为明细标题（前端展示"包含 N 项"）。
 */
public record TaskTemplateVO(
        String code,
        String stage,
        String name,
        String description,
        List<String> items
) {
}
