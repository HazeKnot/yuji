package com.yuji.wellness.community.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.wellness.community.dto.CommunityDtos;
import com.yuji.wellness.community.service.CommunityService;
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
@RequestMapping("/api/wellness/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public Result<CommunityDtos.PageVO<CommunityDtos.PostVO>> page(
            @AuthenticationPrincipal LoginUser me,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(communityService.page(me.userId(), category, page, size));
    }

    @GetMapping("/posts/{id}")
    public Result<CommunityDtos.PostVO> detail(@AuthenticationPrincipal LoginUser me,
                                               @PathVariable Long id) {
        return Result.ok(communityService.detail(me.userId(), id));
    }

    @PostMapping("/posts")
    public Result<CommunityDtos.PostVO> create(@AuthenticationPrincipal LoginUser me,
                                               @Valid @RequestBody CommunityDtos.PostReq req) {
        return Result.ok(communityService.create(me.userId(), req));
    }

    @GetMapping("/posts/{id}/comments")
    public Result<List<CommunityDtos.CommentVO>> comments(@PathVariable Long id) {
        return Result.ok(communityService.comments(id));
    }

    @PostMapping("/posts/{id}/comments")
    public Result<CommunityDtos.CommentVO> comment(@AuthenticationPrincipal LoginUser me,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody CommunityDtos.CommentReq req) {
        return Result.ok(communityService.addComment(me.userId(), id, req));
    }

    @PostMapping("/posts/{id}/like")
    public Result<CommunityDtos.LikeVO> like(@AuthenticationPrincipal LoginUser me,
                                             @PathVariable Long id) {
        return Result.ok(communityService.toggleLike(me.userId(), id));
    }
}
