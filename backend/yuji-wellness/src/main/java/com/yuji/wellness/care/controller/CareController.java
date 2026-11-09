package com.yuji.wellness.care.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.care.dto.CareDtos;
import com.yuji.wellness.care.service.CareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wellness/care")
@RequiredArgsConstructor
public class CareController {

    private final CareService careService;

    /** 没有计划时返回 null，前端据此展示"生成月子计划"表单 */
    @GetMapping("/plan")
    public Result<CareDtos.PlanVO> current(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(careService.current(me.userId()));
    }

    @PostMapping("/plans")
    public Result<CareDtos.PlanVO> create(@AuthenticationPrincipal LoginUser me,
                                          @Valid @RequestBody CareDtos.PlanReq req) {
        return Result.ok(careService.createOrReplace(me.userId(), req));
    }

    @PutMapping("/tasks/{id}")
    public Result<CareDtos.TaskVO> mark(@AuthenticationPrincipal LoginUser me,
                                        @PathVariable Long id,
                                        @Valid @RequestBody CareDtos.StatusReq req) {
        return Result.ok(careService.markTask(me.userId(), id, req.status()));
    }
}
