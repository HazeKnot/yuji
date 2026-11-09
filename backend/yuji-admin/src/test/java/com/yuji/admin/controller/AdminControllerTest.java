package com.yuji.admin.controller;

import com.yuji.admin.dto.DashboardVO;
import com.yuji.admin.service.AdminDashboardService;
import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 机构端接口契约：保证 {code,msg,data} 结构与权限错误码不跑偏。
 */
class AdminControllerTest {

    private final AdminDashboardService service = mock(AdminDashboardService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MvcTestSupport.standalone(new AdminController(service));
    }

    @AfterEach
    void tearDown() {
        MvcTestSupport.logout();
    }

    @Test
    @DisplayName("看板返回聚合数字与近 7 天趋势")
    void shouldReturnDashboard() throws Exception {
        MvcTestSupport.login(new LoginUser(100L, "ORG"));
        when(service.overview(any())).thenReturn(new DashboardVO(
                3, 2, 2, 3, 10, 1, 8, 3, 38, 20, 5, 25,
                List.of(new DashboardVO.WeekBucket("孕早期", 1)),
                List.of(new DashboardVO.DayCount("2026-11-03", 4)),
                DashboardVO.NOTE));

        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.momCount").value(3))
                .andExpect(jsonPath("$.data.taskRate").value(38))
                .andExpect(jsonPath("$.data.weekBuckets[0].label").value("孕早期"))
                .andExpect(jsonPath("$.data.recentRecords[0].count").value(4));
    }

    @Test
    @DisplayName("非机构账号：业务码 40300，HTTP 仍是 200（前端统一按 code 处理）")
    void shouldRejectNonOrg() throws Exception {
        MvcTestSupport.login(new LoginUser(1L, "MOM"));
        when(service.overview(any()))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "该接口仅限机构账号访问"));

        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40300));
    }
}
