package com.yuji.wellness.support;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;

public enum PostCategory {

    EXPERIENCE("经验分享", "过来人的真实经验，不替代医生建议"),
    SECRET("难言之隐", "匿名说出来，身体的问题不需要羞耻"),
    CONFINEMENT("月子日记", "月子里的具体问题与解法"),
    PARENTING("育儿互助", "喂养、睡眠、辅食、如厕"),
    MOOD("心理树洞", "情绪需要一个出口");

    private final String label;
    private final String intro;

    PostCategory(String label, String intro) {
        this.label = label;
        this.intro = intro;
    }

    public String label() {
        return label;
    }

    public String intro() {
        return intro;
    }

    public static PostCategory of(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的社区分类");
        }
    }

    public static String labelOf(String value) {
        if (value == null) {
            return "";
        }
        try {
            return valueOf(value).label;
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
