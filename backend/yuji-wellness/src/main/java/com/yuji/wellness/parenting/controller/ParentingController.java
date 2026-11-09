package com.yuji.wellness.parenting.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.parenting.dto.ParentingDtos;
import com.yuji.wellness.parenting.service.ParentingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wellness/parenting")
@RequiredArgsConstructor
public class ParentingController {

    private final ParentingService parentingService;

    @GetMapping("/courses")
    public Result<ParentingDtos.PageVO> courses(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(parentingService.page(me.userId()));
    }

    @GetMapping("/courses/{id}")
    public Result<ParentingDtos.CourseDetailVO> detail(@PathVariable Long id) {
        return Result.ok(parentingService.detail(id));
    }
}
