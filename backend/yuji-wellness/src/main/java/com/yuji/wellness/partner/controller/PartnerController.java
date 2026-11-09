package com.yuji.wellness.partner.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.partner.dto.PartnerDtos;
import com.yuji.wellness.partner.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wellness/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping
    public Result<List<PartnerDtos.OrgVO>> list(@RequestParam(required = false) String city,
                                                @RequestParam(required = false) String type) {
        return Result.ok(partnerService.list(city, type));
    }

    @GetMapping("/{id}")
    public Result<PartnerDtos.OrgDetailVO> detail(@PathVariable Long id) {
        return Result.ok(partnerService.detail(id));
    }

    /** 预约/咨询线索：提交后进入机构端线索池，平台不参与诊疗与收费 */
    @PostMapping("/{id}/leads")
    public Result<PartnerDtos.LeadVO> lead(@AuthenticationPrincipal LoginUser me,
                                           @PathVariable Long id,
                                           @Valid @RequestBody PartnerDtos.LeadReq req) {
        return Result.ok(partnerService.submitLead(me.userId(), id, req));
    }
}
