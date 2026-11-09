package com.yuji.admin.dto;

import java.util.List;

/**
 * 机构端看板：全部为聚合数字，不含任何用户标识、指标值或情绪内容。
 *
 * <p>边界说明写进 {@code note}，前端直接展示——机构看到的是"多少人在什么阶段"，
 * 而不是"谁怎么样了"。
 */
public record DashboardVO(
        int momCount,
        int familyCount,
        int memberCount,
        int profileCount,
        int recordCount,
        int reportCount,
        int taskTotal,
        int taskDone,
        int taskRate,
        int checkupTotal,
        int checkupDone,
        int checkupRate,
        List<WeekBucket> weekBuckets,
        List<DayCount> recentRecords,
        String note
) {

    public static final String NOTE = "仅聚合统计，不含任何个人明细；数字为实时查询，刷新即最新。";

    /** 孕周分桶（按末次月经推算，仅用于资源配置参考） */
    public record WeekBucket(String label, int count) {
    }

    /** 近 7 天指标记录条数（缺失日期补 0，便于直接画图） */
    public record DayCount(String date, int count) {
    }
}
