package com.yuji.wellness.support;

public enum CareCategory {

    DIET("饮食"),
    RECOVERY("康复"),
    REST("作息"),
    FEEDING("喂养"),
    MOOD("情绪"),
    CHECKUP("复查"),
    CONTRACEPTION("避孕");

    private final String label;

    CareCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static String labelOf(String value) {
        try {
            return valueOf(value).label;
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }
}
