package com.yuji.wellness.profile.dto;

import com.yuji.wellness.care.dto.CareDtos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class WellnessDtos {

    private WellnessDtos() {
    }

    public record ProfileReq(
            String lifeStage,
            @Size(max = 20, message = "昵称最长 20 字") String nickname,
            LocalDate lastPeriodDate,
            @Min(21) @Max(45) Integer cycleLength,
            @Min(2) @Max(10) Integer periodLength,
            LocalDate babyBirthDate,
            LocalDate deliveryDate,
            String deliveryMode,
            @Size(max = 10) List<String> concerns
    ) {
    }

    public record ProfileVO(
            String lifeStage,
            String lifeStageLabel,
            LocalDate lastPeriodDate,
            Integer cycleLength,
            Integer periodLength,
            LocalDate babyBirthDate,
            LocalDate deliveryDate,
            String deliveryMode,
            List<String> concerns
    ) {
    }

    /**
     * 首页聚合：不要求用户已建档，前端据 hasProfile 决定是否跳引导。
     */
    public record OverviewVO(
            Boolean hasProfile,
            String nickname,
            String lifeStage,
            String lifeStageLabel,
            Integer cycleDay,
            String phase,
            String phaseLabel,
            String phaseHint,
            LocalDate nextPeriodDate,
            Long daysToNextPeriod,
            Integer avgCycleLength,
            Integer avgPeriodLength,
            Boolean pregnant,
            Integer weeks,
            Integer days,
            Long daysToEdd,
            String pregnancyStage,
            Boolean postpartum,
            Integer babyAgeMonths,
            Integer careDayIndex,
            String careStageLabel,
            List<CareDtos.TaskVO> todayCareTasks,
            String todayTip
    ) {
    }
}
