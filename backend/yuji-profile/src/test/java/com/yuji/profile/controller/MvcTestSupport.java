package com.yuji.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yuji.common.GlobalExceptionHandler;
import com.yuji.common.security.LoginUser;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

/**
 * Controller 层测试脚手架（不启动 Spring 容器）：
 * 配置 JavaTime（LocalDate 序列化）、@AuthenticationPrincipal 解析器与统一异常处理。
 */
final class MvcTestSupport {

    private MvcTestSupport() {
    }

    static MockMvc standalone(Object controller) {
        ObjectMapper om = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                // JSON 为主；Resource 转换器用于报告下载（InputStreamResource → 文件流）
                .setMessageConverters(new MappingJackson2HttpMessageConverter(om),
                        new ByteArrayHttpMessageConverter(),
                        new ResourceHttpMessageConverter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    /**
     * 模拟"已登录用户"。
     *
     * <p>注意：这里直接写 SecurityContextHolder，不用 spring-security-test 的
     * {@code authentication()} 后置处理器——后者会切换 SecurityContextHolder 的存储策略，
     * 而 {@link AuthenticationPrincipalArgumentResolver} 在构建时已捕获旧策略，会读不到上下文。
     */
    static void login(LoginUser user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    static void logout() {
        SecurityContextHolder.clearContext();
    }
}
