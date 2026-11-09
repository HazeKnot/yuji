package com.yuji.auth.security;

import com.yuji.common.security.LoginUser;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 过滤器测试：这是全站唯一的鉴权入口，任何"脏 token 被当成合法身份"的漏洞都在这里。
 */
class JwtAuthFilterTest {

    private JwtService jwtService;
    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final AtomicBoolean chainInvoked = new AtomicBoolean(false);
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthFilter(jwtService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chainInvoked.set(false);
        chain = (req, res) -> chainInvoked.set(true);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("没有 Authorization 头：不放行身份，但请求继续往下走（由授权规则决定 401）")
    void shouldPassThroughWithoutHeader() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(chainInvoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).parse(anyString());
    }

    @Test
    @DisplayName("非 Bearer 前缀（如 Basic）不解析，避免误把其它凭据当 JWT")
    void shouldIgnoreNonBearerHeader() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(chainInvoked).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).parse(anyString());
    }

    @Test
    @DisplayName("合法 Bearer token：把 LoginUser 注入 SecurityContext")
    void shouldAuthenticateWithValidToken() throws Exception {
        LoginUser loginUser = new LoginUser(42L, "MOM");
        when(jwtService.parse("good-token")).thenReturn(loginUser);
        request.addHeader("Authorization", "Bearer good-token");

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(loginUser);
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(chainInvoked).isTrue();
    }

    @Test
    @DisplayName("脏 token：清空上下文并继续放行，绝不产生登录态")
    void shouldClearContextOnInvalidToken() throws Exception {
        // 模拟同一线程上残留的上一次请求上下文，验证坏 token 不会“继承”前一个身份
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(1L, "ADMIN"), null, List.of()));
        when(jwtService.parse("bad-token")).thenThrow(new MalformedJwtException("bad"));
        request.addHeader("Authorization", "Bearer bad-token");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chainInvoked).isTrue();
        assertThat(response.getStatus()).as("过滤器自身不返回 401，交给入口点").isEqualTo(200);
    }
}
