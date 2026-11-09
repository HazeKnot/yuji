package com.yuji.profile.controller;

import com.yuji.common.security.LoginUser;
import com.yuji.common.security.Roles;
import com.yuji.profile.dto.ReportVO;
import com.yuji.profile.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import static com.yuji.profile.controller.MvcTestSupport.login;
import static com.yuji.profile.controller.MvcTestSupport.logout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 报告接口测试：multipart 上传、下载响应头（文件名编码）、删除归属。
 */
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final LoginUser ME = new LoginUser(7L, Roles.MOM);

    @Mock
    private ReportService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MvcTestSupport.standalone(new ReportController(service));
        login(ME);
    }

    @AfterEach
    void tearDown() {
        logout();
    }

    @Test
    @DisplayName("POST /api/checkups/{id}/reports：multipart 上传成功")
    void shouldUpload() throws Exception {
        when(service.upload(eq(7L), eq(3L), any())).thenReturn(new ReportVO(1L, 3L, "唐筛.pdf",
                "application/pdf", 12, "/api/reports/1/download", LocalDateTime.of(2026, 11, 3, 9, 0)));

        MockMultipartFile file = new MockMultipartFile("file", "唐筛.pdf", "application/pdf", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/checkups/3/reports").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileName").value("唐筛.pdf"))
                .andExpect(jsonPath("$.data.downloadUrl").value("/api/reports/1/download"));
    }

    @Test
    @DisplayName("GET /api/checkups/{id}/reports：按产检项列报告")
    void shouldListByCheckup() throws Exception {
        when(service.list(7L, 3L)).thenReturn(List.of(new ReportVO(1L, 3L, "a.pdf",
                "application/pdf", 1, "/api/reports/1/download", null)));

        mvc.perform(get("/api/checkups/3/reports"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/reports：列出我的全部报告")
    void shouldListMine() throws Exception {
        when(service.list(7L, null)).thenReturn(List.of());

        mvc.perform(get("/api/reports"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("下载：返回文件字节 + attachment 头（中文名走 RFC 5987 编码）")
    void shouldDownload() throws Exception {
        byte[] bytes = "pdf-bytes".getBytes();
        when(service.open(7L, 1L)).thenReturn(new ReportService.ReportFile(
                new ReportVO(1L, 3L, "唐筛.pdf", "application/pdf", bytes.length,
                        "/api/reports/1/download", null),
                new ByteArrayInputStream(bytes)));

        mvc.perform(get("/api/reports/1/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName("DELETE /api/reports/{id}：透传当前用户与报告 id")
    void shouldDelete() throws Exception {
        mvc.perform(delete("/api/reports/9"))
                .andExpect(jsonPath("$.code").value(0));

        verify(service).delete(7L, 9L);
    }
}
