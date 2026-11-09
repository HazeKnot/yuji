package com.yuji.wellness.support;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;

public enum PartnerType {

    HOSPITAL("妇幼保健院"),
    MATERNITY("月子中心"),
    POSTPARTUM("产后康复"),
    COMMUNITY("社区机构");

    private final String label;

    PartnerType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PartnerType of(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的机构类型");
        }
    }

    public static String labelOf(String value) {
        try {
            return valueOf(value).label;
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }
}
