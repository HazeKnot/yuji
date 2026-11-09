package com.yuji.wellness.support;

/**
 * 周期四阶段。只用于饮食与自我照护建议，不用于判断排卵或诊断。
 */
public enum CyclePhase {

    MENSTRUAL("经期", "补铁、温热、易消化，先照顾好当下"),
    FOLLICULAR("卵泡期", "精力回升，优质蛋白和叶酸安排上"),
    OVULATION("排卵期", "抗氧化与锌硒，注意分泌物变化"),
    LUTEAL("黄体期", "镁与 B6，控糖少盐，给情绪留空间");

    private final String label;
    private final String hint;

    CyclePhase(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    public String label() {
        return label;
    }

    public String hint() {
        return hint;
    }
}
