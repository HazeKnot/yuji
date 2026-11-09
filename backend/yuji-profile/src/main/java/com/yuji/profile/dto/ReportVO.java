package com.yuji.profile.dto;

import java.time.LocalDateTime;

/**
 * 报告元数据。downloadUrl 需要带 token 访问（前端用 blob 下载，避免把地址直接暴露在公网）。
 */
public record ReportVO(
        Long id,
        Long checkupItemId,
        String fileName,
        String contentType,
        long size,
        String downloadUrl,
        LocalDateTime createdAt
) {
}
