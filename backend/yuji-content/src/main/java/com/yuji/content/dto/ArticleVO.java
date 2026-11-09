package com.yuji.content.dto;

/**
 * 详情：带正文、来源与免责声明。来源字段是内容合规的硬要求，不允许为空。
 */
public record ArticleVO(
        Long id,
        String code,
        String title,
        String category,
        String categoryLabel,
        int minWeek,
        int maxWeek,
        String weekHint,
        String summary,
        String body,
        String source,
        String reviewer,
        String disclaimer
) {
}
