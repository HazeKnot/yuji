package com.yuji.wellness.support;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;

/**
 * 女性全周期阶段。产品不再假设"所有用户都在孕期"。
 */
public enum LifeStage {

    CYCLE("经期管理"),
    PREPARE("备孕"),
    PREGNANT("孕期"),
    POSTPARTUM("产后"),
    PARENTING("育儿"),
    MENOPAUSE("更年期");

    private final String label;

    LifeStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static LifeStage of(String value) {
        if (value == null || value.isBlank()) {
            return CYCLE;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的生命阶段");
        }
    }
}
