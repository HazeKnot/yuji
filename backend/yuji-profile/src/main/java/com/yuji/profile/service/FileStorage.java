package com.yuji.profile.service;

import java.io.InputStream;

/**
 * 报告文件存储抽象。
 *
 * <p>默认本地磁盘（开发/单机零依赖），配置 {@code yuji.storage.type=minio} 即切到对象存储。
 * 业务代码只依赖本接口，因此"换存储"不需要改报告逻辑。
 */
public interface FileStorage {

    void store(String objectKey, InputStream in, long size, String contentType);

    InputStream open(String objectKey);

    void delete(String objectKey);

    /** 实现标识，落到 report.storage 字段，便于排查"文件到底在哪" */
    String type();
}
