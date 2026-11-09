package com.yuji.wellness.cycle.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.cycle.dto.CycleDtos;
import com.yuji.wellness.cycle.service.CycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wellness/cycles")
@RequiredArgsConstructor
public class CycleController {

    private final CycleService cycleService;

    @GetMapping
    public Result<List<CycleDtos.RecordVO>> list(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(cycleService.list(me.userId()));
    }

    @PostMapping
    public Result<CycleDtos.RecordVO> add(@AuthenticationPrincipal LoginUser me,
                                         @Valid @RequestBody CycleDtos.RecordReq req) {
        return Result.ok(cycleService.add(me.userId(), req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        cycleService.delete(me.userId(), id);
        return Result.ok();
    }

    @GetMapping("/summary")
    public Result<CycleDtos.SummaryVO> summary(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(cycleService.summary(me.userId()));
    }
}
