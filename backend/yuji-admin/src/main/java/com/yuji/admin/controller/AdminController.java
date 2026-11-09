package com.yuji.admin.controller;

import com.yuji.admin.dto.DashboardVO;
import com.yuji.admin.service.AdminDashboardService;
import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;

    /** 机构端看板（聚合统计，仅 ORG / ADMIN） */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(dashboardService.overview(me));
    }
}
