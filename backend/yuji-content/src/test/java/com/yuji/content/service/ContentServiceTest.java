package com.yuji.content.service;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.content.TableInfoInitializer;
import com.yuji.content.dto.ArticleBriefVO;
import com.yuji.content.dto.ArticleVO;
import com.yuji.content.entity.Article;
import com.yuji.content.mapper.ArticleMapper;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.service.ProfileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 内容推荐的正确性主要在两件事：孕周区间命中、以及"离当前孕周最近"的排序。
 */
@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ContentService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoInitializer.init(Article.class);
    }

    @Test
    @DisplayName("未建档：40401（不返回空列表，前端据此跳建档引导）")
    void shouldRejectWithoutProfile() {
        when(profileService.getMine(1L)).thenThrow(new BusinessException(ErrorCode.NO_PROFILE));

        assertThatThrownBy(() -> service.forMe(1L, 3))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.NO_PROFILE.getCode());
        verifyNoInteractions(articleMapper);
    }

    @Test
    @DisplayName("按孕周推荐：只留区间命中，且离当前孕周最近的排第一")
    void shouldRecommendNearestFirst() {
        when(profileService.getMine(1L)).thenReturn(profile(20));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article("EARLY", 0, 13), article("MID", 18, 24), article("LATE", 28, 42)));

        List<ArticleBriefVO> rows = service.forMe(1L, 3);

        assertThat(rows).extracting(ArticleBriefVO::code).containsExactly("MID", "EARLY", "LATE");
        assertThat(rows.get(0).weekHint()).isEqualTo("孕 18–24 周");
        assertThat(rows.get(0).categoryLabel()).isEqualTo("产检");
        assertThat(rows.get(0).source()).isNotBlank();
    }

    @Test
    @DisplayName("limit 归一：0 走默认 3，超过上限按 50 截断")
    void shouldNormalizeLimit() {
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article("A", 0, 13), article("B", 0, 13), article("C", 0, 13), article("D", 0, 13)));

        assertThat(service.forWeek(10, 0)).hasSize(3);
        assertThat(service.forWeek(10, 999)).hasSize(4);
    }

    @Test
    @DisplayName("孕周非法：40000，不查库")
    void shouldRejectInvalidWeeks() {
        assertThatThrownBy(() -> service.forWeek(-1, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("孕周");
        assertThatThrownBy(() -> service.list(null, 61))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("孕周");
        verifyNoInteractions(articleMapper);
    }

    @Test
    @DisplayName("未知分类：40000（避免大小写/拼写错误被静默忽略成「全部」）")
    void shouldRejectUnknownCategory() {
        assertThatThrownBy(() -> service.list("SLEEP", null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("不筛选时按孕周先后排：早期内容在前")
    void shouldOrderByWeeksWhenNoFilter() {
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article("LATE", 28, 42), article("EARLY", 0, 13), article("MID", 14, 27)));

        assertThat(service.list(null, null)).extracting(ArticleBriefVO::code)
                .containsExactly("EARLY", "MID", "LATE");
    }

    @Test
    @DisplayName("详情：正文与来源齐全，并带免责声明")
    void shouldReturnDetail() {
        Article a = article("OGTT", 24, 28);
        a.setId(7L);
        a.setBody("正文");
        a.setReviewer("内容组");
        when(articleMapper.selectById(7L)).thenReturn(a);

        ArticleVO vo = service.detail(7L);

        assertThat(vo.title()).isEqualTo("OGTT");
        assertThat(vo.body()).isEqualTo("正文");
        assertThat(vo.reviewer()).isEqualTo("内容组");
        assertThat(vo.disclaimer()).isEqualTo(ContentService.DISCLAIMER);
        assertThat(vo.weekHint()).isEqualTo("孕 24–28 周");
    }

    @Test
    @DisplayName("详情：不存在 / 已下线 / 已删除都按 40400 处理，不泄露存在性")
    void shouldHideMissingArticle() {
        when(articleMapper.selectById(1L)).thenReturn(null);
        Article offline = article("X", 0, 13);
        offline.setPublished(false);
        when(articleMapper.selectById(2L)).thenReturn(offline);
        Article removed = article("Y", 0, 13);
        removed.setDeletedAt(java.time.LocalDateTime.now());
        when(articleMapper.selectById(3L)).thenReturn(removed);

        for (long id : new long[]{1L, 2L, 3L}) {
            assertThatThrownBy(() -> service.detail(id))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        }
        assertThatThrownBy(() -> service.detail(null))
                .isInstanceOf(BusinessException.class);
        verify(articleMapper, org.mockito.Mockito.never()).selectList(any());
    }

    @Test
    @DisplayName("分类编码容错：大小写不敏感；未知编码原样展示，不做二次猜测")
    void shouldParseCategorySafely() {
        assertThat(com.yuji.content.dto.ArticleCategory.of("nutrition"))
                .isEqualTo(com.yuji.content.dto.ArticleCategory.NUTRITION);
        assertThat(com.yuji.content.dto.ArticleCategory.of(null)).isNull();
        assertThat(com.yuji.content.dto.ArticleCategory.labelOf("SLEEP")).isEqualTo("SLEEP");
    }

    // ---------- 夹具 ----------

    private static Article article(String code, int min, int max) {
        Article a = new Article();
        a.setCode(code);
        a.setTitle(code);
        a.setCategory("CHECKUP");
        a.setMinWeek(min);
        a.setMaxWeek(max);
        a.setSummary("摘要");
        a.setSource("《孕前和孕期保健指南（2018）》");
        a.setPublished(true);
        return a;
    }

    private static ProfileVO profile(int weeks) {
        return new ProfileVO(1L, "小满", LocalDate.now().minusDays(weeks * 7L),
                LocalDate.now().plusDays(280 - weeks * 7L), List.of(), weeks, 0,
                weeks <= 13 ? "孕早期" : weeks <= 27 ? "孕中期" : "孕晚期",
                280 - weeks * 7L, false);
    }
}
