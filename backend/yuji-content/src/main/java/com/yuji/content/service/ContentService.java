package com.yuji.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.content.dto.ArticleBriefVO;
import com.yuji.content.dto.ArticleCategory;
import com.yuji.content.dto.ArticleVO;
import com.yuji.content.entity.Article;
import com.yuji.content.mapper.ArticleMapper;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 知识内容：按孕周区间命中。
 *
 * <p>排序不是「最新发布」，而是「离当前孕周最近」——同一段孕周里，
 * 让人先看到此刻最相关的那一篇。
 */
@Service
@RequiredArgsConstructor
public class ContentService {

    /** 每篇正文页脚统一展示：内容不等于诊疗建议 */
    public static final String DISCLAIMER = "内容为健康教育信息，不替代医生诊断。任何不适请及时就医。";

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 50;

    private final ArticleMapper articleMapper;
    private final ProfileService profileService;

    /** 首页「本周知识」：按当前孕周推荐，未建档直接 40401（与其它妈妈端接口一致） */
    public List<ArticleBriefVO> forMe(Long userId, int limit) {
        ProfileVO me = profileService.getMine(userId);
        return forWeek(me.weeks(), limit);
    }

    public List<ArticleBriefVO> forWeek(int weeks, int limit) {
        requireValidWeeks(weeks);
        List<Article> rows = articleMapper.selectList(
                base().le(Article::getMinWeek, weeks).ge(Article::getMaxWeek, weeks));
        return rows.stream()
                .sorted(relevance(weeks))
                .limit(normalizeLimit(limit))
                .map(this::toBrief)
                .toList();
    }

    /** 知识库列表：可按分类与孕周筛选，都不传就是全部 */
    public List<ArticleBriefVO> list(String category, Integer weeks) {
        LambdaQueryWrapper<Article> wrapper = base();
        if (category != null && !category.isBlank()) {
            ArticleCategory c = ArticleCategory.of(category);
            if (c == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "未知的内容分类");
            }
            wrapper.eq(Article::getCategory, c.name());
        }
        if (weeks != null) {
            requireValidWeeks(weeks);
            wrapper.le(Article::getMinWeek, weeks).ge(Article::getMaxWeek, weeks);
        }
        return articleMapper.selectList(wrapper).stream()
                .sorted(relevance(weeks))
                .map(this::toBrief)
                .toList();
    }

    public ArticleVO detail(Long id) {
        Article a = id == null ? null : articleMapper.selectById(id);
        if (a == null || a.getDeletedAt() != null || Boolean.FALSE.equals(a.getPublished())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在或已下线");
        }
        return new ArticleVO(a.getId(), a.getCode(), a.getTitle(), a.getCategory(),
                ArticleCategory.labelOf(a.getCategory()), a.getMinWeek(), a.getMaxWeek(),
                weekHint(a), a.getSummary(), a.getBody(), a.getSource(), a.getReviewer(),
                DISCLAIMER);
    }

    // ---------- 内部 ----------

    private LambdaQueryWrapper<Article> base() {
        return new LambdaQueryWrapper<Article>()
                .isNull(Article::getDeletedAt)
                .eq(Article::getPublished, true);
    }

    private ArticleBriefVO toBrief(Article a) {
        return new ArticleBriefVO(a.getId(), a.getCode(), a.getTitle(), a.getCategory(),
                ArticleCategory.labelOf(a.getCategory()), a.getMinWeek(), a.getMaxWeek(),
                weekHint(a), a.getSummary(), a.getSource());
    }

    static String weekHint(Article a) {
        int min = a.getMinWeek();
        int max = a.getMaxWeek();
        return min == max ? "孕 " + min + " 周" : "孕 " + min + "–" + max + " 周";
    }

    /** weeks 为空时按孕周先后排（越早的阶段越靠前），否则按区间中点距离排序 */
    private static Comparator<Article> relevance(Integer weeks) {
        if (weeks == null) {
            return Comparator.comparingInt(Article::getMinWeek);
        }
        return Comparator
                .comparingInt((Article a) -> Math.abs(midWeek(a) - weeks))
                .thenComparingInt(Article::getMinWeek);
    }

    private static int midWeek(Article a) {
        return (a.getMinWeek() + a.getMaxWeek()) / 2;
    }

    private static void requireValidWeeks(int weeks) {
        if (weeks < 0 || weeks > 60) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "孕周超出合理范围");
        }
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
