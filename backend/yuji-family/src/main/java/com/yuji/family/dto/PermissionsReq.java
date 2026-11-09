package com.yuji.family.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 字段级授权请求：只允许指标类型（体重/血压/血糖/胎动）。
 * 情绪记录、量表得分不在可授权范围内，传了也会被服务端拒绝。
 */
public record PermissionsReq(
        @Size(max = 10, message = "授权字段过多")
        List<String> visibleFields,

        @Size(max = 20, message = "称呼不能超过 20 字")
        String relation
) {
}
