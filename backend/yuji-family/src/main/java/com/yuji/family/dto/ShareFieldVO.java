package com.yuji.family.dto;

/**
 * 可授权的指标字段。清单由后端给出，避免前端硬编码与后端漂移。
 */
public record ShareFieldVO(String code, String label, String unit) {
}
