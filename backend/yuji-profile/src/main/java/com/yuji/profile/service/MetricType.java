package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;

/**
 * 指标类型及其校验/展示元数据。
 *
 * <p>把"合法范围、单位、参考区间"集中在一处，避免散落在 controller 与前端：
 * 后端做硬校验（防脏数据），前端只负责提示文案。
 *
 * <p>⚠️ 参考区间仅用于折线图底色，**不构成医学判读**，接口会返回免责声明字段。
 */
public enum MetricType {

    /** 体重：kg */
    WEIGHT("体重", "kg", 20, 250, null, null, null, null),

    /** 血压：primary=收缩压，secondary=舒张压 */
    BLOOD_PRESSURE("血压", "mmHg", 40, 260, 90d, 140d, 30d, 160d),

    /** 血糖：mmol/L */
    BLOOD_GLUCOSE("血糖", "mmol/L", 1, 30, 3.9, 6.1, null, null),

    /** 胎动：次/小时 */
    FETAL_MOVEMENT("胎动", "次/小时", 0, 100, null, null, null, null);

    private final String label;
    private final String unit;
    private final double min;
    private final double max;
    private final Double refMin;
    private final Double refMax;
    private final Double secondMin;
    private final Double secondMax;

    MetricType(String label, String unit, double min, double max,
               Double refMin, Double refMax, Double secondMin, Double secondMax) {
        this.label = label;
        this.unit = unit;
        this.min = min;
        this.max = max;
        this.refMin = refMin;
        this.refMax = refMax;
        this.secondMin = secondMin;
        this.secondMax = secondMax;
    }

    public String label() {
        return label;
    }

    public String unit() {
        return unit;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public Double refMin() {
        return refMin;
    }

    public Double refMax() {
        return refMax;
    }

    /** 是否必须提供第二个值（目前只有血压的舒张压） */
    public boolean requiresSecond() {
        return secondMin != null;
    }

    public Double secondMin() {
        return secondMin;
    }

    public Double secondMax() {
        return secondMax;
    }

    /**
     * 解析类型码。用循环而不是 {@code valueOf}，保证 null / 未知值都返回明确的 40000，
     * 而不是 NPE 或 500。
     */
    public static MetricType parse(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请指定指标类型");
        }
        for (MetricType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的指标类型：" + code);
    }
}
