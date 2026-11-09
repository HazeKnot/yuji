package com.yuji.content.controller;

import com.yuji.common.Result;
import com.yuji.common.security.LoginUser;
import com.yuji.content.dto.ArticleBriefVO;
import com.yuji.content.dto.ArticleVO;
import com.yuji.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    /** 知识库：?category=NUTRITION&weeks=20，两个参数都可省略 */
    @GetMapping
    public Result<List<ArticleBriefVO>> list(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) Integer weeks) {
        return Result.ok(contentService.list(category, weeks));
    }

    /** 首页「本周知识」：按当前孕周推荐 */
    @GetMapping("/for-me")
    public Result<List<ArticleBriefVO>> forMe(@AuthenticationPrincipal LoginUser me,
                                              @RequestParam(defaultValue = "3") int limit) {
        return Result.ok(contentService.forMe(me.userId(), limit));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id) {
        return Result.ok(contentService.detail(id));
    }
}
