package com.yuji.wellness.meal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.wellness.cycle.dto.CycleDtos;
import com.yuji.wellness.cycle.service.CycleService;
import com.yuji.wellness.meal.dto.MealDtos;
import com.yuji.wellness.meal.entity.MealTemplate;
import com.yuji.wellness.meal.mapper.MealTemplateMapper;
import com.yuji.wellness.support.CyclePhase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 月事餐饮规划：按周期阶段生成一周食谱 + 购物清单。
 *
 * <p>食谱是模板拼接，不做个体化营养处方；所有页面都带"不替代医生/营养师"的免责声明。
 */
@Service
@RequiredArgsConstructor
public class MealService {

    public static final String DISCLAIMER = "食谱为日常饮食建议，不替代医生或营养师对贫血、糖尿病、食物过敏等情况的个体化指导。";

    private static final String[] WEEKDAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final MealTemplateMapper templateMapper;
    private final CycleService cycleService;

    public MealDtos.PlanVO current(Long userId, LocalDate anyDayInWeek) {
        LocalDate weekStart = mondayOf(anyDayInWeek);
        LocalDate weekEnd = weekStart.plusDays(6);

        CycleDtos.SummaryVO summary = cycleService.summary(userId);
        CyclePhase phase;
        String hint;
        if (summary == null || summary.phase() == null) {
            phase = CyclePhase.FOLLICULAR;
            hint = "还没有足够的周期记录，先给你一份均衡食谱；记录月经后会自动切换到对应阶段。";
        } else {
            phase = CyclePhase.valueOf(summary.phase());
            hint = phase.hint();
        }

        List<MealTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<MealTemplate>()
                        .eq(MealTemplate::getPhase, phase.name())
                        .orderByAsc(MealTemplate::getDayOffset));
        if (templates.isEmpty()) {
            hint = "餐饮模板正在补充中，本周先按日常均衡饮食安排。";
        }

        List<MealDtos.DayVO> days = new ArrayList<>();
        Set<String> shopping = new LinkedHashSet<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            MealTemplate t = templates.isEmpty() ? null : templates.get(i % templates.size());
            List<MealDtos.MealVO> meals = new ArrayList<>();
            if (t != null) {
                meals.add(new MealDtos.MealVO("BREAKFAST", "早餐", t.getBreakfast()));
                meals.add(new MealDtos.MealVO("LUNCH", "午餐", t.getLunch()));
                meals.add(new MealDtos.MealVO("DINNER", "晚餐", t.getDinner()));
                if (t.getSnack() != null && !t.getSnack().isBlank()) {
                    meals.add(new MealDtos.MealVO("SNACK", "加餐", t.getSnack()));
                }
                shopping.addAll(splitIngredients(t.getIngredients()));
            }
            days.add(new MealDtos.DayVO(date, WEEKDAYS[i], i + 1, meals,
                    t == null ? "均衡饮食" : t.getNutrients(),
                    t == null ? "按自己的胃口和过敏情况调整。" : t.getTips()));
        }

        return new MealDtos.PlanVO(phase.name(), phase.label(), hint, weekStart, weekEnd,
                days, new ArrayList<>(shopping), DISCLAIMER);
    }

    static LocalDate mondayOf(LocalDate day) {
        LocalDate base = day == null ? LocalDate.now() : day;
        return base.with(DayOfWeek.MONDAY);
    }

    static List<String> splitIngredients(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split("、"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
