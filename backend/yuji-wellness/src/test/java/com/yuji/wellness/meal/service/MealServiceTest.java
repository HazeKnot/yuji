package com.yuji.wellness.meal.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MealServiceTest {

    @Test
    void 任意一天都归一到所在周的周一() {
        assertThat(MealService.mondayOf(LocalDate.of(2026, 11, 4)))
                .isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(MealService.mondayOf(LocalDate.of(2026, 11, 8)))
                .isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void 食材按顿号拆分并去空() {
        assertThat(MealService.splitIngredients("小米、红枣、、鸡蛋"))
                .containsExactly("小米", "红枣", "鸡蛋");
        assertThat(MealService.splitIngredients(null)).isEqualTo(List.of());
    }
}
