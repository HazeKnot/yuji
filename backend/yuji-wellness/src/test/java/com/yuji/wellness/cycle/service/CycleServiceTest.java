package com.yuji.wellness.cycle.service;

import com.yuji.wellness.cycle.dto.CycleDtos;
import com.yuji.wellness.cycle.entity.CycleRecord;
import com.yuji.wellness.support.CyclePhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CycleServiceTest {

    @Test
    void 四个阶段按周期长度分段() {
        assertThat(CycleService.phaseOf(1, 28, 5)).isEqualTo(CyclePhase.MENSTRUAL);
        assertThat(CycleService.phaseOf(5, 28, 5)).isEqualTo(CyclePhase.MENSTRUAL);
        assertThat(CycleService.phaseOf(6, 28, 5)).isEqualTo(CyclePhase.FOLLICULAR);
        assertThat(CycleService.phaseOf(13, 28, 5)).isEqualTo(CyclePhase.OVULATION);
        assertThat(CycleService.phaseOf(17, 28, 5)).isEqualTo(CyclePhase.OVULATION);
        assertThat(CycleService.phaseOf(18, 28, 5)).isEqualTo(CyclePhase.LUTEAL);
    }

    @Test
    void 短周期也不会出现空档() {
        assertThat(CycleService.phaseOf(4, 21, 5)).isEqualTo(CyclePhase.MENSTRUAL);
        assertThat(CycleService.phaseOf(6, 21, 5)).isIn(CyclePhase.FOLLICULAR, CyclePhase.OVULATION);
        assertThat(CycleService.phaseOf(21, 21, 5)).isEqualTo(CyclePhase.LUTEAL);
    }

    @Test
    void 平均周期只统计合理区间() {
        List<LocalDate> starts = List.of(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 30),  // 29 天
                LocalDate.of(2026, 3, 1));  // 30 天
        assertThat(CycleService.averageCycle(starts, 28)).isEqualTo(30);
    }

    @Test
    void 没有历史记录时使用档案默认值() {
        LocalDate today = LocalDate.of(2026, 11, 4);
        CycleDtos.SummaryVO summary = CycleService.buildSummary(List.of(),
                LocalDate.of(2026, 10, 20), 28, 5, today);

        assertThat(summary.avgCycleLength()).isEqualTo(28);
        assertThat(summary.cycleDay()).isEqualTo(16);
        assertThat(summary.phase()).isEqualTo("OVULATION");
        assertThat(summary.daysToNextPeriod()).isEqualTo(13);
    }

    @Test
    void 记录过期太久时不硬猜当前周期() {
        LocalDate today = LocalDate.of(2026, 11, 4);
        CycleDtos.SummaryVO summary = CycleService.buildSummary(List.of(),
                LocalDate.of(2026, 6, 1), 28, 5, today);

        assertThat(summary.cycleDay()).isNull();
        assertThat(summary.phase()).isNull();
    }

    @Test
    void 预测未来三次月经() {
        LocalDate today = LocalDate.of(2026, 11, 4);
        CycleDtos.SummaryVO summary = CycleService.buildSummary(List.of(),
                LocalDate.of(2026, 10, 20), 28, 5, today);

        List<CycleDtos.PredictionVO> predictions = summary.predictions();
        assertThat(predictions).hasSize(3);
        assertThat(predictions.get(0).startDate()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(predictions.get(1).startDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(predictions.get(0).endDate()).isEqualTo(LocalDate.of(2026, 11, 21));
    }

    @Test
    void 平均经期只统计有结束日的记录() {
        List<CycleRecord> rows = new ArrayList<>();
        rows.add(record(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)));
        rows.add(record(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4)));
        rows.add(record(LocalDate.of(2026, 10, 29), null));

        assertThat(CycleService.averagePeriod(rows, 5)).isEqualTo(5);
    }

    @Test
    void 经量标签走白名单() {
        assertThat(CycleService.flowLabel("LIGHT")).isEqualTo("少量");
        assertThat(CycleService.flowLabel("HEAVY")).isEqualTo("较多");
        assertThat(CycleService.flowLabel("NORMAL")).isEqualTo("中等");
    }

    private static CycleRecord record(LocalDate start, LocalDate end) {
        CycleRecord row = new CycleRecord();
        row.setStartDate(start);
        row.setEndDate(end);
        return row;
    }
}
