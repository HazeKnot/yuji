package com.yuji.profile.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 孕周计算器单元测试（纯函数，无需 Spring 上下文）。
 *
 * <p>断言全部基于 {@code LocalDate.now()} 的相对日期构造，因此不依赖运行日期，任何时候跑都稳定。
 * 覆盖重点：13+6/14+0、27+6/28+0 三个阶段边界、280/281 天产后边界、未来日期与异常输入。
 */
class GestationalCalculatorTest {

    private static final LocalDate TODAY = LocalDate.now();

    /** 以"距今天数"构造末次月经日期 */
    private static LocalDate lmpDaysAgo(long days) {
        return TODAY.minusDays(days);
    }

    @ParameterizedTest(name = "距末次月经 {0} 天 → 孕 {1} 周 {2} 天，阶段 {3}")
    @CsvSource({
            // 天数, 周, 天, 阶段
            "0,   0,  0, 孕早期",
            "4,   0,  4, 孕早期",
            "45,  6,  3, 孕早期",
            "90,  12, 6, 孕早期",
            "97,  13, 6, 孕早期",   // 孕早期最后一天
            "98,  14, 0, 孕中期",   // 孕中期第一天
            "195, 27, 6, 孕中期",   // 孕中期最后一天
            "196, 28, 0, 孕晚期",   // 孕晚期第一天
            "252, 36, 0, 孕晚期",
            "280, 40, 0, 孕晚期"    // 预产期当天，尚不算产后
    })
    @DisplayName("按天数换算孕周与阶段，覆盖各阶段边界")
    void shouldConvertDaysToWeeksAndStage(long daysAgo, int expectWeeks, int expectDays, String expectStage) {
        LocalDate lmp = lmpDaysAgo(daysAgo);

        GestationalCalculator.Info info = GestationalCalculator.fromLmp(lmp);

        assertThat(info.weeks()).isEqualTo(expectWeeks);
        assertThat(info.days()).isEqualTo(expectDays);
        assertThat(info.stage()).isEqualTo(expectStage);
        assertThat(info.postpartum()).isFalse();
        assertThat(info.edd()).isEqualTo(lmp.plusDays(280));
    }

    @Test
    @DisplayName("预产期 = 末次月经 + 280 天，距预产期天数可正可负")
    void shouldComputeEddAndCountdown() {
        LocalDate lmp = lmpDaysAgo(196);

        GestationalCalculator.Info info = GestationalCalculator.fromLmp(lmp);

        assertThat(info.edd()).isEqualTo(lmp.plusDays(280));
        assertThat(info.daysToEdd()).isEqualTo(84);      // 280 - 196
        assertThat(ChronoUnit.DAYS.between(TODAY, info.edd())).isEqualTo(84);
    }

    @Test
    @DisplayName("预产期当天：孕 40 周整、倒计时 0 天、尚未标记为产后")
    void shouldBeDueTodayAtDay280() {
        GestationalCalculator.Info info = GestationalCalculator.fromLmp(lmpDaysAgo(280));

        assertThat(info.weeks()).isEqualTo(40);
        assertThat(info.days()).isZero();
        assertThat(info.daysToEdd()).isZero();
        assertThat(info.postpartum()).isFalse();
    }

    @Test
    @DisplayName("超过 280 天视为产后：孕周钳制在 40 周、阶段为产后、倒计时为负")
    void shouldMarkPostpartumAfterDay280() {
        LocalDate lmp = lmpDaysAgo(300);

        GestationalCalculator.Info info = GestationalCalculator.fromLmp(lmp);

        assertThat(info.postpartum()).isTrue();
        assertThat(info.stage()).isEqualTo("产后");
        assertThat(info.weeks()).isEqualTo(40);
        assertThat(info.days()).isZero();
        assertThat(info.daysToEdd()).isEqualTo(-20);
    }

    @Test
    @DisplayName("末次月经在未来（误填）：不抛异常，孕周钳制为 0+0")
    void shouldClampNegativeDaysToZero() {
        GestationalCalculator.Info info = GestationalCalculator.fromLmp(lmpDaysAgo(-10));

        assertThat(info.weeks()).isZero();
        assertThat(info.days()).isZero();
        assertThat(info.stage()).isEqualTo("孕早期");
        assertThat(info.postpartum()).isFalse();
        assertThat(info.daysToEdd()).isEqualTo(290);
    }

    @ParameterizedTest(name = "不变量：距末次月经 {0} 天时 weeks*7+days 应等于 min(max(天数,0),280)")
    @CsvSource({ "-5", "0", "1", "97", "98", "196", "279", "280", "281", "400" })
    @DisplayName("孕周与天数始终自洽（不会出现 41+3 之类的越界值）")
    void weeksAndDaysShouldStayConsistent(long daysAgo) {
        GestationalCalculator.Info info = GestationalCalculator.fromLmp(lmpDaysAgo(daysAgo));

        long clamped = Math.min(Math.max(daysAgo, 0), 280);
        assertThat(info.weeks() * 7L + info.days()).isEqualTo(clamped);
        assertThat(info.weeks()).isBetween(0, 40);
        assertThat(info.days()).isBetween(0, 6);
    }

    @Test
    @DisplayName("由预产期反推末次月经：edd - 280 天，且与 fromLmp 互为逆运算")
    void shouldDeriveLmpFromEdd() {
        LocalDate edd = LocalDate.of(2027, 1, 25);

        LocalDate lmp = GestationalCalculator.lmpFromEdd(edd);

        assertThat(lmp).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(GestationalCalculator.fromLmp(lmp).edd()).isEqualTo(edd);
    }
}
