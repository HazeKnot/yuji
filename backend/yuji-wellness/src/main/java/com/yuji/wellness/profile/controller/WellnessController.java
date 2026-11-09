package com.yuji.wellness.profile.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.profile.dto.WellnessDtos;
import com.yuji.wellness.profile.service.WellnessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wellness")
@RequiredArgsConstructor
public class WellnessController {

    private final WellnessService wellnessService;

    /** 建档/更新全周期档案：经期、备孕、孕期、产后、育儿、更年期 */
    @PostMapping("/profiles")
    public Result<WellnessDtos.ProfileVO> save(@AuthenticationPrincipal LoginUser me,
                                               @Valid @RequestBody WellnessDtos.ProfileReq req) {
        return Result.ok(wellnessService.saveProfile(me.userId(), req));
    }

    /** 首页聚合：档案、周期、孕周、月子进度、今日任务一次返回 */
    @GetMapping("/overview")
    public Result<WellnessDtos.OverviewVO> overview(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(wellnessService.overview(me.userId()));
    }
}
