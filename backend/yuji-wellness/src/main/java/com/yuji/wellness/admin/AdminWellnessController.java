package com.yuji.wellness.admin;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.community.dto.CommunityDtos;
import com.yuji.wellness.community.service.CommunityService;
import com.yuji.wellness.mental.dto.MentalDtos;
import com.yuji.wellness.mental.service.MentalService;
import com.yuji.wellness.partner.dto.PartnerDtos;
import com.yuji.wellness.partner.service.PartnerService;
import com.yuji.wellness.support.AdminGuard;
import com.yuji.wellness.support.OrgScope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 机构端能力域接口：工单跟进、社区审核、合作机构与预约线索。
 * 所有接口都经 AdminGuard 校验 ORG/ADMIN 角色，且不下发用户健康明细。
 */
@RestController
@RequestMapping("/api/admin/wellness")
@RequiredArgsConstructor
public class AdminWellnessController {

    private final AdminWellnessService adminService;
    private final MentalService mentalService;
    private final CommunityService communityService;
    private final PartnerService partnerService;
    private final OrgScope orgScope;

    @GetMapping("/overview")
    public Result<PartnerDtos.AdminOverviewVO> overview(@AuthenticationPrincipal LoginUser me) {
        AdminGuard.requireOrg(me);
        return Result.ok(adminService.overview());
    }

    // ---------- 会员管理（SaaS 租户） ----------

    @GetMapping("/members")
    public Result<List<PartnerDtos.MemberVO>> members(@AuthenticationPrincipal LoginUser me,
                                                      @RequestParam(required = false) String keyword) {
        Long orgId = orgScope.resolve(me);
        return Result.ok(adminService.members(orgId, keyword));
    }

    @GetMapping("/members/{id}")
    public Result<PartnerDtos.MemberDetailVO> memberDetail(@AuthenticationPrincipal LoginUser me,
                                                           @PathVariable Long id) {
        Long orgId = orgScope.resolve(me);
        return Result.ok(adminService.memberDetail(orgId, id));
    }

    // ---------- AI 风控告警 ----------

    @GetMapping("/alerts")
    public Result<List<MentalDtos.TicketVO>> alerts(@AuthenticationPrincipal LoginUser me,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String level) {
        Long orgId = orgScope.resolve(me);
        return Result.ok(adminService.alerts(orgId, status, level));
    }

    @GetMapping("/tickets")
    public Result<List<MentalDtos.TicketVO>> tickets(@AuthenticationPrincipal LoginUser me,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String level) {
        Long orgId = orgScope.resolve(me);
        if (orgId == null) {
            return Result.ok(mentalService.adminTickets(status, level));
        }
        return Result.ok(mentalService.adminTickets(status, level, orgScope.memberIds(orgId)));
    }

    @PutMapping("/tickets/{id}")
    public Result<MentalDtos.TicketVO> updateTicket(@AuthenticationPrincipal LoginUser me,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody PartnerDtos.TicketStatusReq req) {
        AdminGuard.requireOrg(me);
        return Result.ok(adminService.updateTicket(id, req));
    }

    @GetMapping("/posts")
    public Result<CommunityDtos.PageVO<CommunityDtos.PostVO>> posts(
            @AuthenticationPrincipal LoginUser me,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireOrg(me);
        return Result.ok(communityService.adminPage(status, page, size));
    }

    @PutMapping("/posts/{id}")
    public Result<Void> updatePost(@AuthenticationPrincipal LoginUser me,
                                   @PathVariable Long id,
                                   @Valid @RequestBody PartnerDtos.PostStatusReq req) {
        AdminGuard.requireOrg(me);
        communityService.updateStatus(id, req.status());
        return Result.ok();
    }

    @GetMapping("/partners")
    public Result<List<PartnerDtos.OrgVO>> partners(@AuthenticationPrincipal LoginUser me) {
        AdminGuard.requireOrg(me);
        return Result.ok(partnerService.adminList());
    }

    @GetMapping("/partners/{id}")
    public Result<PartnerDtos.OrgDetailVO> partner(@AuthenticationPrincipal LoginUser me,
                                                   @PathVariable Long id) {
        AdminGuard.requireOrg(me);
        return Result.ok(partnerService.adminDetail(id));
    }

    @PostMapping("/partners")
    public Result<PartnerDtos.OrgDetailVO> createPartner(@AuthenticationPrincipal LoginUser me,
                                                         @Valid @RequestBody PartnerDtos.OrgReq req) {
        AdminGuard.requireOrg(me);
        return Result.ok(partnerService.saveOrg(null, req));
    }

    @PutMapping("/partners/{id}")
    public Result<PartnerDtos.OrgDetailVO> updatePartner(@AuthenticationPrincipal LoginUser me,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody PartnerDtos.OrgReq req) {
        AdminGuard.requireOrg(me);
        return Result.ok(partnerService.saveOrg(id, req));
    }

    @DeleteMapping("/partners/{id}")
    public Result<Void> archivePartner(@AuthenticationPrincipal LoginUser me, @PathVariable Long id) {
        AdminGuard.requireOrg(me);
        partnerService.archiveOrg(id);
        return Result.ok();
    }

    @GetMapping("/leads")
    public Result<List<PartnerDtos.LeadVO>> leads(@AuthenticationPrincipal LoginUser me,
                                                  @RequestParam(required = false) Long orgId,
                                                  @RequestParam(required = false) String status) {
        Long scope = orgScope.resolve(me);
        // ORG 租户强制只能看自己的线索；ADMIN 可按 orgId 筛选全平台
        return Result.ok(partnerService.adminLeads(scope != null ? scope : orgId, status));
    }

    @PutMapping("/leads/{id}")
    public Result<PartnerDtos.LeadVO> updateLead(@AuthenticationPrincipal LoginUser me,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody PartnerDtos.LeadStatusReq req) {
        AdminGuard.requireOrg(me);
        return Result.ok(partnerService.updateLead(id, req));
    }
}
