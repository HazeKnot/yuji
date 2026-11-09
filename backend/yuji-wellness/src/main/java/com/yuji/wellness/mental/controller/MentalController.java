package com.yuji.wellness.mental.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.mental.dto.MentalDtos;
import com.yuji.wellness.mental.service.MentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wellness/mental")
@RequiredArgsConstructor
public class MentalController {

    private final MentalService mentalService;

    @GetMapping("/overview")
    public Result<MentalDtos.OverviewVO> overview(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(mentalService.overview(me.userId()));
    }

    @PostMapping("/checkins")
    public Result<MentalDtos.CheckinVO> checkin(@AuthenticationPrincipal LoginUser me,
                                                @Valid @RequestBody MentalDtos.CheckinReq req) {
        return Result.ok(mentalService.checkin(me.userId(), req));
    }

    /** EPDS 10 题：结果只做风险分层，黄档生成陪伴工单，红档建议转介 */
    @PostMapping("/scales/epds")
    public Result<MentalDtos.EpdsResultVO> epds(@AuthenticationPrincipal LoginUser me,
                                                @Valid @RequestBody MentalDtos.EpdsReq req) {
        return Result.ok(mentalService.submitEpds(me.userId(), req));
    }

    @GetMapping("/tickets")
    public Result<List<MentalDtos.TicketVO>> tickets(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(mentalService.myTickets(me.userId()));
    }
}
