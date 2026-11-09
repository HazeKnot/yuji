package com.yuji.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.GlobalExceptionHandler;
import com.yuji.common.security.LoginUser;
import com.yuji.content.dto.ArticleBriefVO;
import com.yuji.content.dto.ArticleVO;
import com.yuji.content.service.ContentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentControllerTest {

    private final ContentService service = mock(ContentService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ObjectMapper om = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mvc = MockMvcBuilders.standaloneSetup(new ContentController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(om))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(1L, "MOM"), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("本周知识：默认 limit=3，返回列表项（不含正文）")
    void shouldReturnForMe() throws Exception {
        when(service.forMe(1L, 3)).thenReturn(List.of(
                new ArticleBriefVO(7L, "OGTT", "糖耐前一晚怎么吃", "CHECKUP", "产检",
                        24, 28, "孕 24–28 周", "摘要", "《妊娠期高血糖诊治指南（2022）》")));

        mvc.perform(get("/api/articles/for-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].code").value("OGTT"))
                .andExpect(jsonPath("$.data[0].weekHint").value("孕 24–28 周"))
                .andExpect(jsonPath("$.data[0].body").doesNotExist());

        verify(service).forMe(1L, 3);
    }

    @Test
    @DisplayName("知识库：分类与孕周参数透传（都不传也能调）")
    void shouldPassFilters() throws Exception {
        when(service.list("NUTRITION", 20)).thenReturn(List.of());

        mvc.perform(get("/api/articles").param("category", "NUTRITION").param("weeks", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(service).list("NUTRITION", 20);
    }

    @Test
    @DisplayName("详情：正文、来源、免责声明齐全")
    void shouldReturnDetail() throws Exception {
        when(service.detail(7L)).thenReturn(new ArticleVO(7L, "OGTT", "糖耐前一晚怎么吃", "CHECKUP",
                "产检", 24, 28, "孕 24–28 周", "摘要", "正文",
                "《妊娠期高血糖诊治指南（2022）》", "内容组", ContentService.DISCLAIMER));

        mvc.perform(get("/api/articles/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文"))
                .andExpect(jsonPath("$.data.source").isNotEmpty())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    @DisplayName("内容不存在：业务码 40400，HTTP 仍 200")
    void shouldMapNotFound() throws Exception {
        when(service.detail(anyLong()))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "内容不存在或已下线"));

        mvc.perform(get("/api/articles/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400));

        verify(service).detail(999L);
        verify(service, org.mockito.Mockito.never()).forMe(anyLong(), anyInt());
    }
}
