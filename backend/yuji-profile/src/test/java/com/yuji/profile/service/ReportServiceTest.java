package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import com.yuji.profile.dto.ReportVO;
import com.yuji.profile.entity.CheckupItem;
import com.yuji.profile.entity.Report;
import com.yuji.profile.mapper.CheckupItemMapper;
import com.yuji.profile.mapper.ReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报告服务测试：类型白名单、体积上限、路径与文件名安全、归属校验、孤儿文件清理。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private CheckupItemMapper checkupItemMapper;

    @Mock
    private FileStorage storage;

    @InjectMocks
    private ReportService service;

    @Test
    @DisplayName("上传成功：对象键由服务端生成（reports/{userId}/uuid.pdf），元数据落库")
    void shouldUploadPdf() {
        when(storage.type()).thenReturn("LOCAL");
        CheckupItem item = item(3L, USER_ID);
        when(checkupItemMapper.selectById(3L)).thenReturn(item);

        ReportVO vo = service.upload(USER_ID, 3L, pdf("唐筛报告.pdf", 3));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insert(captor.capture());
        Report saved = captor.getValue();

        assertThat(saved.getObjectKey())
                .startsWith("reports/7/")
                .endsWith(".pdf")
                .doesNotContain("唐筛");   // 对象键里不出现用户文件名
        assertThat(saved.getFileName()).isEqualTo("唐筛报告.pdf");
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
        assertThat(saved.getSizeBytes()).isEqualTo(3L);
        assertThat(saved.getStorage()).isEqualTo("LOCAL");
        assertThat(saved.getCheckupItemId()).isEqualTo(3L);

        verify(storage).store(eq(saved.getObjectKey()), any(InputStream.class), eq(3L), eq("application/pdf"));
        assertThat(vo.downloadUrl()).startsWith("/api/reports/").endsWith("/download");
    }

    @Test
    @DisplayName("空文件 / 超过 10MB / 类型不在白名单：都是 40000")
    void shouldRejectInvalidFiles() {
        assertThatThrownBy(() -> service.upload(USER_ID, null,
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请选择要上传的文件");

        byte[] tooBig = new byte[(int) ReportService.MAX_SIZE + 1];
        assertThatThrownBy(() -> service.upload(USER_ID, null,
                new MockMultipartFile("file", "big.pdf", "application/pdf", tooBig)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件不能超过 10MB");

        assertThatThrownBy(() -> service.upload(USER_ID, null,
                new MockMultipartFile("file", "x.html", "text/html", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持 JPG / PNG / WEBP / PDF");

        verify(storage, never()).store(anyString(), any(), anyLong(), anyString());
        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    @DisplayName("扩展名由 content type 推导：用户把 .jsp 改名成 .png 也只落成 .png")
    void shouldDeriveExtensionFromContentType() {
        when(storage.type()).thenReturn("LOCAL");

        service.upload(USER_ID, null,
                new MockMultipartFile("file", "shell.jsp", "image/png", new byte[]{1, 2}));

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insert(captor.capture());
        assertThat(captor.getValue().getObjectKey()).endsWith(".png");
        assertThat(captor.getValue().getFileName()).as("展示名保留原样，但不会写进路径").isEqualTo("shell.jsp");
    }

    @Test
    @DisplayName("关联他人的产检项：40400，且不写文件")
    void shouldRejectOthersCheckupItem() {
        when(checkupItemMapper.selectById(3L)).thenReturn(item(3L, 8L));

        assertThatThrownBy(() -> service.upload(USER_ID, 3L, pdf("a.pdf", 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("产检项不存在");

        verify(storage, never()).store(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("元数据写库失败：回删已上传文件（不留孤儿文件）")
    void shouldCleanUpFileWhenInsertFails() {
        when(storage.type()).thenReturn("LOCAL");
        doThrow(new IllegalStateException("db down")).when(reportMapper).insert(any(Report.class));

        assertThatThrownBy(() -> service.upload(USER_ID, null, pdf("a.pdf", 1)))
                .isInstanceOf(IllegalStateException.class);

        verify(storage).delete(anyString());
    }

    @Test
    @DisplayName("文件名清洗：去掉路径、空名兜底")
    void shouldSanitizeFileName() {
        assertThat(ReportService.sanitizeFileName("C:\\Users\\me\\报告 1.pdf", "pdf")).isEqualTo("报告 1.pdf");
        assertThat(ReportService.sanitizeFileName("../../etc/passwd", "pdf")).isEqualTo("passwd");
        assertThat(ReportService.sanitizeFileName(null, "png")).isEqualTo("report.png");
        assertThat(ReportService.sanitizeFileName("   ", "png")).isEqualTo("report.png");
        assertThat(ReportService.sanitizeFileName("a".repeat(200) + ".pdf", "pdf")).hasSize(120);
    }

    @Test
    @DisplayName("删除：越权返回 40400 且不动物理文件")
    void shouldRejectDeletingOthersReport() {
        Report others = report(5L, 8L, "reports/8/x.pdf");
        when(reportMapper.selectById(5L)).thenReturn(others);

        assertThatThrownBy(() -> service.delete(USER_ID, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("报告不存在");

        verify(storage, never()).delete(anyString());
        verify(reportMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除：先删库再删文件；文件删除失败不影响接口成功")
    void shouldDeleteReportAndTolerateStorageFailure() {
        Report mine = report(5L, USER_ID, "reports/7/x.pdf");
        when(reportMapper.selectById(5L)).thenReturn(mine);
        doThrow(new BusinessException(com.yuji.common.ErrorCode.BUSINESS_ERROR, "io"))
                .when(storage).delete("reports/7/x.pdf");

        service.delete(USER_ID, 5L);

        verify(reportMapper).deleteById(5L);
        verify(storage).delete("reports/7/x.pdf");
    }

    @Test
    @DisplayName("下载：他人报告 40400；本人返回元数据与流")
    void shouldOpenOwnReport() {
        Report mine = report(5L, USER_ID, "reports/7/x.pdf");
        when(reportMapper.selectById(5L)).thenReturn(mine);
        when(storage.open("reports/7/x.pdf")).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        ReportService.ReportFile file = service.open(USER_ID, 5L);

        assertThat(file.meta().fileName()).isEqualTo("报告.pdf");
        assertThat(file.stream()).isNotNull();

        when(reportMapper.selectById(6L)).thenReturn(report(6L, 8L, "reports/8/y.pdf"));
        assertThatThrownBy(() -> service.open(USER_ID, 6L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("报告不存在");
    }

    @Test
    @DisplayName("列表：可按产检项过滤；历史数据缺 size 时按 0 处理")
    void shouldListAndTolerateMissingSize() {
        Report withoutSize = report(1L, USER_ID, "reports/7/a.pdf");
        withoutSize.setSizeBytes(null);
        when(reportMapper.selectList(any())).thenReturn(List.of(withoutSize));

        List<ReportVO> list = service.list(USER_ID, 3L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).size()).isZero();
        assertThat(list.get(0).downloadUrl()).isEqualTo("/api/reports/1/download");
    }

    // ---------- 夹具 ----------

    private static MockMultipartFile pdf(String name, int size) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[size]);
    }

    private static CheckupItem item(Long id, Long userId) {
        CheckupItem i = new CheckupItem();
        i.setId(id);
        i.setUserId(userId);
        return i;
    }

    private static Report report(Long id, Long userId, String key) {
        Report r = new Report();
        r.setId(id);
        r.setUserId(userId);
        r.setFileName("报告.pdf");
        r.setObjectKey(key);
        r.setContentType("application/pdf");
        r.setSizeBytes(10L);
        r.setStorage("LOCAL");
        return r;
    }
}
