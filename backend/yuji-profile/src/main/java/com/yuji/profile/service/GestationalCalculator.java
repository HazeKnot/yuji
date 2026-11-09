package com.yuji.profile.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 孕周计算器（纯函数，便于单元测试——答辩演示点之一）。
 *
 * <p>规则：孕周 = 距末次月经天数 / 7；预产期 = 末次月经 + 280 天；
 * 孕早期 ≤13+6，孕中期 14–27+6，孕晚期 ≥28；超过 280 天视为产后。
 */
public final class GestationalCalculator {

    public record Info(
            int weeks,
            int days,
            String stage,
            LocalDate edd,
            long daysToEdd,
            boolean postpartum
    ) {
    }

    private GestationalCalculator() {
    }

    public static Info fromLmp(LocalDate lmp) {
        LocalDate today = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(lmp, today);

        boolean postpartum = totalDays > 280;
        long clamped = Math.max(Math.min(totalDays, 280), 0);
        int weeks = (int) (clamped / 7);
        int days = (int) (clamped % 7);

        LocalDate edd = lmp.plusDays(280);
        String stage = postpartum ? "产后"
                : weeks <= 13 ? "孕早期"
                : weeks <= 27 ? "孕中期"
                : "孕晚期";

        return new Info(weeks, days, stage, edd, ChronoUnit.DAYS.between(today, edd), postpartum);
    }

    /** 只知道预产期时反推末次月经 */
    public static LocalDate lmpFromEdd(LocalDate edd) {
        return edd.minusDays(280);
    }
}
