package com.yuji.wellness.consult.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.consult.dto.ConsultDtos;
import com.yuji.wellness.consult.service.ConsultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wellness/consult")
@RequiredArgsConstructor
public class ConsultController {

    private final ConsultService consultService;

    @GetMapping("/topics")
    public Result<List<ConsultDtos.TopicVO>> topics() {
        return Result.ok(consultService.topics());
    }

    @GetMapping("/sessions")
    public Result<List<ConsultDtos.SessionVO>> sessions(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(consultService.mySessions(me.userId()));
    }

    @PostMapping("/sessions")
    public Result<ConsultDtos.SessionDetailVO> create(@AuthenticationPrincipal LoginUser me,
                                                      @Valid @RequestBody ConsultDtos.SessionReq req) {
        return Result.ok(consultService.create(me.userId(), req));
    }

    @GetMapping("/sessions/{id}")
    public Result<ConsultDtos.SessionDetailVO> detail(@AuthenticationPrincipal LoginUser me,
                                                     @PathVariable Long id) {
        return Result.ok(consultService.detail(me.userId(), id));
    }

    @PostMapping("/sessions/{id}/messages")
    public Result<List<ConsultDtos.MessageVO>> send(@AuthenticationPrincipal LoginUser me,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody ConsultDtos.MessageReq req) {
        return Result.ok(consultService.send(me.userId(), id, req.content()));
    }

    /** 转人工/转介合作机构：生成风险工单，机构端可见 */
    @PostMapping("/sessions/{id}/referral")
    public Result<ConsultDtos.ReferralVO> referral(@AuthenticationPrincipal LoginUser me,
                                                    @PathVariable Long id) {
        return Result.ok(consultService.referral(me.userId(), id));
    }
}
