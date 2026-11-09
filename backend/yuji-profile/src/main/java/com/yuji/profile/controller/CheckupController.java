package com.yuji.profile.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.profile.dto.CheckupVO;
import com.yuji.profile.service.CheckupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkups")
@RequiredArgsConstructor
public class CheckupController {

    private final CheckupService checkupService;

    /** 我的产检日历（按孕周排序，含逾期标记） */
    @GetMapping
    public Result<List<CheckupVO>> list(@AuthenticationPrincipal LoginUser me) {
        return Result.ok(checkupService.listMine(me.userId()));
    }

    /** 标记产检状态：PLANNED / DONE / SKIPPED */
    @PutMapping("/{id}")
    public Result<CheckupVO> updateStatus(@AuthenticationPrincipal LoginUser me,
                                          @PathVariable("id") Long id,
                                          @RequestBody Map<String, String> body) {
        return Result.ok(checkupService.updateStatus(me.userId(), id, body.get("status")));
    }
}
