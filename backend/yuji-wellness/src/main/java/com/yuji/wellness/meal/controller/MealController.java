package com.yuji.wellness.meal.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.meal.dto.MealDtos;
import com.yuji.wellness.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/wellness/meal-plan")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    /** week 可传任意一天，服务端归一到周一 */
    @GetMapping
    public Result<MealDtos.PlanVO> current(@AuthenticationPrincipal LoginUser me,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return Result.ok(mealService.current(me.userId(), week));
    }
}
