package com.yuji.family.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.family.dto.FamilyVO;
import com.yuji.family.dto.InvitationVO;
import com.yuji.family.dto.MemberVO;
import com.yuji.family.dto.PermissionsReq;
import com.yuji.family.dto.ShareFieldVO;
import com.yuji.family.service.FamilyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /** 妈妈生成邀请码（没有家庭时自动创建） */
    @PostMapping("/invitations")
    public Result<InvitationVO> createInvitation(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(familyService.createInvitation(me.userId()));
    }

    /** 家属凭码加入 */
    @PostMapping("/join/{code}")
    public Result<FamilyVO> join(@AuthenticationPrincipal LoginUser me, @PathVariable("code") String code) {
        return Result.ok(familyService.join(me.userId(), code));
    }

    /** 我属于哪个家庭、什么角色（前端据此切换妈妈/家属视图） */
    @GetMapping("/mine")
    public Result<FamilyVO> mine(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(familyService.mine(me.userId()));
    }

    @GetMapping("/members")
    public Result<List<MemberVO>> members(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(familyService.members(me.userId()));
    }

    /** 字段级授权：只接受指标类型，情绪/量表不可授权 */
    @PutMapping("/members/{id}/permissions")
    public Result<MemberVO> permissions(@AuthenticationPrincipal LoginUser me,
                                        @PathVariable("id") Long id,
                                        @Valid @RequestBody PermissionsReq req) {
        return Result.ok(familyService.updatePermissions(me.userId(), id, req));
    }

    @DeleteMapping("/members/{id}")
    public Result<Void> remove(@AuthenticationPrincipal LoginUser me, @PathVariable("id") Long id) {
        familyService.removeMember(me.userId(), id);
        return Result.ok();
    }

    /** 可授权字段清单（前端渲染开关用，避免硬编码漂移） */
    @GetMapping("/share-fields")
    public Result<List<ShareFieldVO>> shareFields() {
        return Result.ok(familyService.shareFields());
    }
}
