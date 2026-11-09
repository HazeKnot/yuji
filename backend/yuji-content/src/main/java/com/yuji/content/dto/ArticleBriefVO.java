package com.yuji.content.dto;

/**
 * 列表项：不含正文，避免首页一次拉几十 KB。
 */
public record ArticleBriefVO(
        Long id,
        String code,
        String title,
        String category,
        String categoryLabel,
        int minWeek,
        int maxWeek,
        String weekHint,
        String summary,
        String source
) {
}
