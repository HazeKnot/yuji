package com.yuji.profile.config;

import com.yuji.profile.service.FileStorage;
import com.yuji.profile.service.LocalFileStorage;
import com.yuji.profile.service.MinioFileStorage;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * 按配置装配存储实现。默认 local，保证 clone 下来零依赖可跑；
 * 有 MinIO 的环境把 {@code yuji.storage.type} 设为 minio 即可。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "yuji.storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(StorageProperties props) {
        Path root = Path.of(props.getLocalDir());
        log.info("报告存储：本地磁盘 {}", root.toAbsolutePath());
        return new LocalFileStorage(root);
    }

    @Bean
    @ConditionalOnProperty(name = "yuji.storage.type", havingValue = "minio")
    public FileStorage minioFileStorage(StorageProperties props) {
        StorageProperties.Minio m = props.getMinio();
        log.info("报告存储：MinIO {} bucket={}", m.getEndpoint(), m.getBucket());
        MinioClient client = MinioClient.builder()
                .endpoint(m.getEndpoint())
                .credentials(m.getAccessKey(), m.getSecretKey())
                .build();
        return new MinioFileStorage(client, m.getBucket());
    }
}
