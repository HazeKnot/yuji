package com.yuji.wellness.meal.dto;

import java.time.LocalDate;
import java.util.List;

public final class MealDtos {

    private MealDtos() {
    }

    public record MealVO(String type, String typeLabel, String title) {
    }

    public record DayVO(
            LocalDate date,
            String weekday,
            Integer dayIndex,
            List<MealVO> meals,
            String nutrients,
            String tips
    ) {
    }

    public record PlanVO(
            String phase,
            String phaseLabel,
            String phaseHint,
            LocalDate weekStart,
            LocalDate weekEnd,
            List<DayVO> days,
            List<String> shoppingList,
            String disclaimer
    ) {
    }
}
