package com.yuji.auth.security;

import com.yuji.auth.controller.AuthController;
import com.yuji.auth.dto.LoginResp;
import com.yuji.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全配置测试（Web 切片，加载真实的 SecurityFilterChain）。
 * 契约：/api/auth/** 匿名可访问；其它接口无 token 直接 401 并返回统一错误体。
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("未登录访问受保护接口：返回 401 与统一错误码 40100")
    void shouldRejectAnonymousRequest() throws Exception {
        mvc.perform(get("/api/checkups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.msg").value("请先登录"));
    }

    @Test
    @DisplayName("伪造 token 也进不去（验签失败即视为未登录）")
    void shouldRejectForgedToken() throws Exception {
        when(jwtService.parse("forged")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        mvc.perform(get("/api/checkups").header("Authorization", "Bearer forged"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    @DisplayName("注册/登录接口匿名可访问（医院社区扫码即可用，不要求先登录）")
    void shouldAllowAnonymousAuthEndpoints() throws Exception {
        when(authService.register(any())).thenReturn(new LoginResp("t-1",
                new LoginResp.UserVO(1L, "13800138000", "小林", "MOM")));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("t-1"));
    }

    @Test
    @DisplayName("健康检查端点匿名可访问（容器探针用）")
    void shouldAllowHealthEndpoint() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound()); // 切片未加载 actuator，但不应是 401
    }
}
