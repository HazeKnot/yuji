package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import java.io.InputStream;

/**
 * MinIO（S3 兼容）实现：桶不存在时自动创建。
 *
 * <p>通过 {@code yuji.storage.type=minio} 启用，配置见 {@code yuji.storage.minio.*}。
 */
public class MinioFileStorage implements FileStorage {

    private final MinioClient client;
    private final String bucket;

    public MinioFileStorage(MinioClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void store(String objectKey, InputStream in, long size, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件上传失败，请稍后重试");
        }
    }

    @Override
    public InputStream open(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告文件不存在或已被删除");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件删除失败，请稍后重试");
        }
    }

    @Override
    public String type() {
        return "MINIO";
    }

    private void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
