package com.yuji.profile.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.profile.dto.HealthRecordReq;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.TrendVO;
import com.yuji.profile.service.HealthRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    /** 录入一条指标（≤30 秒：前端只让填一个数，其余给默认值） */
    @PostMapping
    public Result<HealthRecordVO> record(@AuthenticationPrincipal LoginUser me,
                                        @Valid @RequestBody HealthRecordReq req) {
        return Result.ok(healthRecordService.record(me.userId(), req));
    }

    /** 列表：?type=WEIGHT&from=2026-10-01&to=2026-11-03 */
    @GetMapping
    public Result<List<HealthRecordVO>> list(
            @AuthenticationPrincipal LoginUser me,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(healthRecordService.listMine(me.userId(), type, from, to));
    }

    /** 趋势：默认最近 30 天，按天聚合 */
    @GetMapping("/trend")
    public Result<TrendVO> trend(@AuthenticationPrincipal LoginUser me,
                                @RequestParam String type,
                                @RequestParam(required = false) Integer days) {
        return Result.ok(healthRecordService.trend(me.userId(), type, days));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable("id") Long id) {
        healthRecordService.delete(me.userId(), id);
        return Result.ok();
    }
}
