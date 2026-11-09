package com.yuji.profile.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置：{@code yuji.storage.*}
 */
@Data
@ConfigurationProperties(prefix = "yuji.storage")
public class StorageProperties {

    /** local（默认，磁盘） / minio */
    private String type = "local";

    /** 本地存储根目录 */
    private String localDir = "./data/uploads";

    private Minio minio = new Minio();

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "yuji";
        private String secretKey = "yuji123456";
        private String bucket = "yuji-reports";
    }
}
