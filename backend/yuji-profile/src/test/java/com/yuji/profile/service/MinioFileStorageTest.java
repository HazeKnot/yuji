package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MinIO 实现测试（client 用 mock，不依赖真实对象存储）。
 * 覆盖桶不存在时自动创建、上传参数、异常映射为业务错误码。
 */
class MinioFileStorageTest {

    private static final String BUCKET = "yuji-reports";

    private MinioClient client;
    private MinioFileStorage storage;

    @BeforeEach
    void setUp() {
        client = mock(MinioClient.class);
        storage = new MinioFileStorage(client, BUCKET);
    }

    @Test
    @DisplayName("桶不存在：先创建桶再上传，对象键与 contentType 正确传递")
    void shouldCreateBucketAndUpload() throws Exception {
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        byte[] bytes = "pdf".getBytes();

        storage.store("reports/7/a.pdf", new ByteArrayInputStream(bytes), bytes.length, "application/pdf");

        ArgumentCaptor<MakeBucketArgs> mk = ArgumentCaptor.forClass(MakeBucketArgs.class);
        verify(client).makeBucket(mk.capture());
        assertThat(mk.getValue().bucket()).isEqualTo(BUCKET);

        ArgumentCaptor<PutObjectArgs> put = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(put.capture());
        assertThat(put.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(put.getValue().object()).isEqualTo("reports/7/a.pdf");
    }

    @Test
    @DisplayName("桶已存在：不重复创建")
    void shouldSkipBucketCreation() throws Exception {
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storage.store("reports/7/a.pdf", new ByteArrayInputStream(new byte[]{1}), 1, "image/png");

        verify(client, never()).makeBucket(any(MakeBucketArgs.class));
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("上传异常：转成 50000 业务异常（不把 SDK 细节抛给前端）")
    void shouldWrapUploadError() throws Exception {
        when(client.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(client.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("connect refused"));

        assertThatThrownBy(() -> storage.store("reports/7/a.pdf",
                new ByteArrayInputStream(new byte[]{1}), 1, "application/pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件上传失败，请稍后重试")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(50000);
    }

    @Test
    @DisplayName("下载：透传流；对象不存在映射为 40400")
    void shouldOpenAndMapNotFound() throws Exception {
        GetObjectResponse stream = mock(GetObjectResponse.class);
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(stream);

        assertThat(storage.open("reports/7/a.pdf")).isSameAs(stream);

        when(client.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("NoSuchKey"));
        assertThatThrownBy(() -> storage.open("reports/7/missing.pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在")
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(40400);
    }

    @Test
    @DisplayName("删除：调用 removeObject；失败映射为 50000")
    void shouldDeleteAndWrapError() throws Exception {
        storage.delete("reports/7/a.pdf");
        ArgumentCaptor<RemoveObjectArgs> rm = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client).removeObject(rm.capture());
        assertThat(rm.getValue().object()).isEqualTo("reports/7/a.pdf");

        doThrow(new RuntimeException("boom")).when(client).removeObject(any(RemoveObjectArgs.class));
        assertThatThrownBy(() -> storage.delete("reports/7/a.pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件删除失败，请稍后重试");
    }

    @Test
    @DisplayName("type() = MINIO，用于落库标记")
    void shouldReportType() {
        assertThat(storage.type()).isEqualTo("MINIO");
    }
}
