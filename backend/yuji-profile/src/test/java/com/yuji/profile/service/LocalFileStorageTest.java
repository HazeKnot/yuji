package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地存储测试：真实读写临时目录 + 目录穿越防护。
 */
class LocalFileStorageTest {

    @Test
    @DisplayName("存 → 读 → 删：内容一致，删除后读取返回 404")
    void shouldStoreReadAndDelete(@TempDir Path root) throws IOException {
        LocalFileStorage storage = new LocalFileStorage(root);
        byte[] content = "诊断报告内容".getBytes();

        storage.store("reports/7/a.pdf", new ByteArrayInputStream(content), content.length, "application/pdf");

        assertThat(root.resolve("reports/7/a.pdf")).exists();
        try (InputStream in = storage.open("reports/7/a.pdf")) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }

        storage.delete("reports/7/a.pdf");
        assertThat(root.resolve("reports/7/a.pdf")).doesNotExist();

        assertThatThrownBy(() -> storage.open("reports/7/a.pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("删除不存在的文件不报错（幂等）")
    void shouldIgnoreDeletingMissingFile(@TempDir Path root) {
        new LocalFileStorage(root).delete("reports/7/none.pdf");
    }

    @Test
    @DisplayName("目录穿越：../ 逃不出根目录，直接 40000")
    void shouldRejectPathTraversal(@TempDir Path root) {
        LocalFileStorage storage = new LocalFileStorage(root);

        assertThatThrownBy(() -> storage.open("../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("非法的文件路径");
        assertThatThrownBy(() -> storage.store("../evil.txt",
                new ByteArrayInputStream(new byte[]{1}), 1, "text/plain"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("非法的文件路径");
    }

    @Test
    @DisplayName("type() 用于落库标记，便于排查文件位置")
    void shouldReportType(@TempDir Path root) {
        assertThat(new LocalFileStorage(root).type()).isEqualTo("LOCAL");
    }
}
