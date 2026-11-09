package com.yuji.profile.controller;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;
import com.yuji.profile.dto.CheckupVO;
import com.yuji.profile.service.CheckupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static com.yuji.profile.controller.MvcTestSupport.login;
import static com.yuji.profile.controller.MvcTestSupport.logout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 产检日历接口测试：JSON 契约、登录态注入、错误码映射。
 */
@ExtendWith(MockitoExtension.class)
class CheckupControllerTest {

    private static final LoginUser ME = new LoginUser(7L, Roles.MOM);

    @Mock
    private CheckupService checkupService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MvcTestSupport.standalone(new CheckupController(checkupService));
        login(ME);
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    @DisplayName("GET /api/checkups：按当前登录用户查询并返回日历字段")
    void shouldListCheckupsOfCurrentUser() throws Exception {
        when(checkupService.listMine(7L)).thenReturn(List.of(
                new CheckupVO(1L, "NT", "NT 超声", 12, "孕 11–13+6 周",
                        LocalDate.of(2026, 7, 13), CheckupVO.PLANNED, true, null)));

        mvc.perform(get("/api/checkups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("NT"))
                .andExpect(jsonPath("$.data[0].suggestedWeek").value(12))
                .andExpect(jsonPath("$.data[0].dueDate").value("2026-07-13"))
                .andExpect(jsonPath("$.data[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.data[0].overdue").value(true));
    }

    @Test
    @DisplayName("GET /api/checkups：未建档用户拿到空数组而不是报错")
    void shouldReturnEmptyArray() throws Exception {
        when(checkupService.listMine(7L)).thenReturn(List.of());

        mvc.perform(get("/api/checkups"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("PUT /api/checkups/{id}：把状态与当前用户一起传给业务层")
    void shouldUpdateStatus() throws Exception {
        when(checkupService.updateStatus(7L, 1L, CheckupVO.DONE)).thenReturn(
                new CheckupVO(1L, "NT", "NT 超声", 12, "孕 11–13+6 周",
                        LocalDate.of(2026, 7, 13), CheckupVO.DONE, false, null));

        mvc.perform(put("/api/checkups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.overdue").value(false));

        verify(checkupService).updateStatus(7L, 1L, CheckupVO.DONE);
    }

    @Test
    @DisplayName("PUT 非法状态：业务层拒绝后映射为 40000")
    void shouldMapIllegalStatusTo40000() throws Exception {
        when(checkupService.updateStatus(anyLong(), anyLong(), any()))
                .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "非法的产检状态"));

        mvc.perform(put("/api/checkups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WHATEVER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    @DisplayName("PUT 缺 status 字段：传 null 给业务层并返回 40000（不能变成 50000）")
    void shouldRejectMissingStatusField() throws Exception {
        when(checkupService.updateStatus(7L, 1L, null))
                .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "非法的产检状态"));

        mvc.perform(put("/api/checkups/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));

        verify(checkupService).updateStatus(7L, 1L, null);
    }
}
