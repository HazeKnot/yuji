package com.yuji.profile.controller;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;
import com.yuji.profile.dto.HealthRecordVO;
import com.yuji.profile.dto.TrendVO;
import com.yuji.profile.service.HealthRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.yuji.profile.controller.MvcTestSupport.login;
import static com.yuji.profile.controller.MvcTestSupport.logout;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 指标记录接口测试：JSON 契约、Bean Validation、趋势参数透传。
 */
@ExtendWith(MockitoExtension.class)
class HealthRecordControllerTest {

    private static final LoginUser ME = new LoginUser(7L, Roles.MOM);

    @Mock
    private HealthRecordService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MvcTestSupport.standalone(new HealthRecordController(service));
        login(ME);
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    @DisplayName("POST /api/health-records：返回带单位的记录")
    void shouldRecord() throws Exception {
        when(service.record(eq(7L), any())).thenReturn(new HealthRecordVO(1L, "WEIGHT", "体重",
                new BigDecimal("60.5"), null, "kg", LocalDateTime.of(2026, 11, 3, 8, 0), "晨起"));

        mvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WEIGHT\",\"value\":60.5,\"note\":\"晨起\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.type").value("WEIGHT"))
                .andExpect(jsonPath("$.data.unit").value("kg"))
                .andExpect(jsonPath("$.data.value").value(60.5));
    }

    @Test
    @DisplayName("POST 缺数值：40000 且不进业务层")
    void shouldRejectMissingValue() throws Exception {
        mvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"WEIGHT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.msg").value(containsString("value")));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /api/health-records：类型与日期区间透传")
    void shouldList() throws Exception {
        when(service.listMine(7L, "WEIGHT", java.time.LocalDate.of(2026, 10, 1), null))
                .thenReturn(List.of());

        mvc.perform(get("/api/health-records").param("type", "WEIGHT").param("from", "2026-10-01"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /api/health-records/trend：返回点序列与免责声明")
    void shouldReturnTrend() throws Exception {
        when(service.trend(7L, "WEIGHT", 7)).thenReturn(new TrendVO("WEIGHT", "体重", "kg",
                List.of(new TrendVO.Point("2026-11-02", new BigDecimal("61"), null)),
                null, null,
                new TrendVO.Summary(new BigDecimal("61"), new BigDecimal("61"), new BigDecimal("61"),
                        new BigDecimal("61"), 1),
                TrendVO.DISCLAIMER));

        mvc.perform(get("/api/health-records/trend").param("type", "WEIGHT").param("days", "7"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.points[0].date").value("2026-11-02"))
                .andExpect(jsonPath("$.data.summary.count").value(1))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    @Test
    @DisplayName("GET trend 业务校验失败：40000")
    void shouldMapTrendError() throws Exception {
        when(service.trend(7L, "NOPE", null))
                .thenThrow(new BusinessException(ErrorCode.PARAM_ERROR, "不支持的指标类型：NOPE"));

        mvc.perform(get("/api/health-records/trend").param("type", "NOPE"))
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    @DisplayName("DELETE /api/health-records/{id}：透传当前用户")
    void shouldDelete() throws Exception {
        mvc.perform(delete("/api/health-records/5"))
                .andExpect(jsonPath("$.code").value(0));

        verify(service).delete(7L, 5L);
    }
}
