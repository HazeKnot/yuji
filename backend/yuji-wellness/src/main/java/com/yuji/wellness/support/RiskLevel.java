package com.yuji.wellness.support;

public enum RiskLevel {

    GREEN("绿档", "自助内容与规律记录即可"),
    YELLOW("黄档", "建议人工陪伴与随访"),
    RED("红档", "建议尽快转介专业机构");

    private final String label;
    private final String action;

    RiskLevel(String label, String action) {
        this.label = label;
        this.action = action;
    }

    public String label() {
        return label;
    }

    public String action() {
        return action;
    }

    public static RiskLevel of(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GREEN;
        }
    }
}
