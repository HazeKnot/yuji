package com.yuji.profile.config;

import com.yuji.profile.service.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 存储装配测试：一行配置切实现，是"换存储不改业务代码"的验证点。
 */
class StorageConfigTest {

    private final StorageConfig config = new StorageConfig();

    @Test
    @DisplayName("默认装配本地磁盘实现")
    void shouldBuildLocalStorage(@TempDir Path dir) {
        StorageProperties props = new StorageProperties();
        props.setType("local");
        props.setLocalDir(dir.toString());

        FileStorage storage = config.localFileStorage(props);

        assertThat(storage.type()).isEqualTo("LOCAL");
    }

    @Test
    @DisplayName("配置 minio 时装配 MinIO 实现（构造客户端不发起网络请求）")
    void shouldBuildMinioStorage() {
        StorageProperties props = new StorageProperties();
        props.setType("minio");
        props.getMinio().setEndpoint("http://localhost:9000");
        props.getMinio().setBucket("yuji-reports");

        FileStorage storage = config.minioFileStorage(props);

        assertThat(storage.type()).isEqualTo("MINIO");
    }
}
