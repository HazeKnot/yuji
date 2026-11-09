package com.yuji.profile.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 趋势图数据：按天聚合后的点序列 + 参考区间 + 统计摘要。
 *
 * <p>注意 {@link #DISCLAIMER}：曲线只做可视化，不做判读（合规章节口径一致）。
 */
public record TrendVO(
        String type,
        String typeLabel,
        String unit,
        List<Point> points,
        Double refMin,
        Double refMax,
        Summary summary,
        String disclaimer
) {

    public static final String DISCLAIMER = "曲线仅用于自我观察，不构成医学判读；如出现明显异常请及时咨询医生。";

    /** 一天一个点（同日多次取最后一次） */
    public record Point(String date, BigDecimal value, BigDecimal value2) {
    }

    /** 摘要：最新 / 最低 / 最高 / 平均 / 点数 */
    public record Summary(BigDecimal latest, BigDecimal min, BigDecimal max, BigDecimal avg, int count) {
    }
}
