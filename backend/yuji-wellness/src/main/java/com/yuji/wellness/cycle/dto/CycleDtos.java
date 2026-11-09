package com.yuji.wellness.cycle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CycleDtos {

    private CycleDtos() {
    }

    public record RecordReq(
            @NotNull LocalDate startDate,
            LocalDate endDate,
            String flow,
            @Size(max = 8) List<String> symptoms,
            String mood,
            @Size(max = 300) String note
    ) {
    }

    public record RecordVO(
            Long id,
            LocalDate startDate,
            LocalDate endDate,
            Integer periodDays,
            String flow,
            String flowLabel,
            List<String> symptoms,
            String mood,
            String note,
            LocalDateTime createdAt
    ) {
    }

    public record PredictionVO(
            LocalDate startDate,
            LocalDate endDate,
            Integer cycleDay
    ) {
    }

    public record SummaryVO(
            LocalDate lastStartDate,
            Integer cycleDay,
            String phase,
            String phaseLabel,
            String phaseHint,
            Integer avgCycleLength,
            Integer avgPeriodLength,
            LocalDate nextPeriodDate,
            Long daysToNextPeriod,
            List<PredictionVO> predictions,
            List<RecordVO> records,
            String disclaimer
    ) {
    }
}
