package com.yuji.family.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.family.dto.SharedSummaryVO;
import com.yuji.family.service.SharedService;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家属视角的只读接口（全部按字段级授权过滤）。
 */
@RestController
@RequestMapping("/api/family/shared")
@RequiredArgsConstructor
public class SharedController {

    private final SharedService sharedService;

    /** 状态摘要：默认可见，不含情绪与量表 */
    @GetMapping("/summary")
    public Result<SharedSummaryVO> summary(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(sharedService.summary(me.userId()));
    }

    /** 已授权指标的记录列表 */
    @GetMapping("/records")
    public Result<List<HealthRecordVO>> records(@AuthenticationPrincipal LoginUser me,
                                                @RequestParam(required = false) String type) {
        return Result.ok(sharedService.records(me.userId(), type));
    }

    /** 已授权指标的趋势 */
    @GetMapping("/trend")
    public Result<TrendVO> trend(@AuthenticationPrincipal LoginUser me,
                                @RequestParam String type,
                                @RequestParam(required = false) Integer days) {
        return Result.ok(sharedService.trend(me.userId(), type, days));
    }
}
