package com.yuji.profile.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.profile.dto.ProfileCreateReq;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 建档（自动生成全孕期产检计划） */
    @PostMapping
    public Result<ProfileVO> create(@AuthenticationPrincipal LoginUser me,
                                    @Valid @RequestBody ProfileCreateReq req) {
        return Result.ok(profileService.create(me.userId(), req));
    }

    /** 我的档案（含孕周、阶段、距预产期天数；未建档返回 40401） */
    @GetMapping("/mine")
    public Result<ProfileVO> mine(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(profileService.getMine(me.userId()));
    }
}
