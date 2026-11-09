package com.yuji.auth.controller;

import com.yuji.auth.dto.LoginResp;
import com.yuji.auth.service.AuthService;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册/登录接口测试（standalone MockMvc，不启动 Spring 容器）。
 * 重点验证：接口层参数校验真的生效（@Valid 不是装饰）、错误码符合前端约定。
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("手机号格式不合法：返回 40000，且根本不会调用业务层")
    void shouldRejectIllegalPhone() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"12345\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.msg").value(containsString("phone")))
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("密码少于 6 位：返回 40000 并指出 password 字段")
    void shouldRejectShortPassword() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"password\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.msg").value(containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("注册成功：返回 code=0 与 token")
    void shouldReturnTokenOnRegister() throws Exception {
        when(authService.register(any())).thenReturn(new LoginResp("token-1",
                new LoginResp.UserVO(1L, "13800138000", "小林", "MOM")));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"password\":\"secret123\",\"nickname\":\"小林\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("token-1"))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.role").value("MOM"));
    }

    @Test
    @DisplayName("业务异常（手机号已注册）：返回 40900")
    void shouldMapBusinessExceptionToCode() throws Exception {
        when(authService.register(any()))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "该手机号已注册"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    @DisplayName("登录成功：返回 code=0 与用户信息")
    void shouldLoginSuccessfully() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResp("token-2",
                new LoginResp.UserVO(5L, "13800138000", "小林", "MOM")));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("token-2"))
                .andExpect(jsonPath("$.data.user.id").value(5));
    }
}
