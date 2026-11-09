package com.yuji.content.dto;

/**
 * 内容分类：只用于展示与筛选，加新分类不改表结构（存的是字符串）。
 */
public enum ArticleCategory {

    NUTRITION("营养"),
    CHECKUP("产检"),
    BODY("身体"),
    MIND("情绪"),
    PREPARE("待产");

    private final String label;

    ArticleCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 未知编码返回 null，由调用方决定是报错还是原样展示 */
    public static ArticleCategory of(String code) {
        if (code == null) {
            return null;
        }
        for (ArticleCategory c : values()) {
            if (c.name().equalsIgnoreCase(code.trim())) {
                return c;
            }
        }
        return null;
    }

    public static String labelOf(String code) {
        ArticleCategory c = of(code);
        return c == null ? code : c.label;
    }
}
