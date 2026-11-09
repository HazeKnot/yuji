package com.yuji.profile.controller;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;
import com.yuji.profile.dto.ProfileVO;
import com.yuji.profile.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static com.yuji.profile.controller.MvcTestSupport.login;
import static com.yuji.profile.controller.MvcTestSupport.logout;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 建档接口测试：参数校验、未建档 40401 契约、当前用户注入。
 */
@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private static final LoginUser ME = new LoginUser(9L, Roles.MOM);

    @Mock
    private ProfileService profileService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MvcTestSupport.standalone(new ProfileController(profileService));
        login(ME);
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    @DisplayName("POST /api/profiles：建档成功返回孕周摘要")
    void shouldCreateProfile() throws Exception {
        when(profileService.create(eq(9L), any())).thenReturn(new ProfileVO(
                1L, "小林", LocalDate.of(2026, 4, 20), LocalDate.of(2027, 1, 25),
                List.of("产检提醒"), 28, 0, "孕晚期", 84L, false));

        mvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lmpDate\":\"2026-04-20\",\"concerns\":[\"产检提醒\"]}"
                                .getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.weeks").value(28))
                .andExpect(jsonPath("$.data.days").value(0))
                .andExpect(jsonPath("$.data.edd").value("2027-01-25"))
                .andExpect(jsonPath("$.data.daysToEdd").value(84))
                .andExpect(jsonPath("$.data.postpartum").value(false));
    }

    @Test
    @DisplayName("POST /api/profiles：lmp 与 edd 都没填时返回 40000")
    void shouldRejectEmptyDates() throws Exception {
        when(profileService.create(eq(9L), any())).thenThrow(
                new BusinessException(ErrorCode.PARAM_ERROR, "请填写末次月经日期或预产期"));

        mvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"小林\"}".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    @DisplayName("POST /api/profiles：昵称超过 20 字被接口层拦住（不会进业务层）")
    void shouldRejectTooLongNickname() throws Exception {
        String nickname = "一二三四五六七八九十一二三四五六七八九十一"; // 21 个字
        String body = "{\"lmpDate\":\"2026-04-20\",\"nickname\":\"" + nickname + "\"}";

        mvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.msg").value(containsString("nickname")));

        verifyNoInteractions(profileService);
    }

    @Test
    @DisplayName("GET /api/profiles/mine：未建档返回 40401（前端据此跳建档引导）")
    void shouldReturn40401WhenNoProfile() throws Exception {
        when(profileService.getMine(9L)).thenThrow(new BusinessException(ErrorCode.NO_PROFILE));

        mvc.perform(get("/api/profiles/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/profiles/mine：已建档返回孕周与阶段")
    void shouldReturnMyProfile() throws Exception {
        when(profileService.getMine(9L)).thenReturn(new ProfileVO(
                1L, "小林", LocalDate.of(2026, 8, 1), LocalDate.of(2027, 5, 8),
                List.of(), 13, 6, "孕早期", 195L, false));

        mvc.perform(get("/api/profiles/mine"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.weeks").value(13))
                .andExpect(jsonPath("$.data.days").value(6))
                .andExpect(jsonPath("$.data.daysToEdd").value(195));
    }
}
