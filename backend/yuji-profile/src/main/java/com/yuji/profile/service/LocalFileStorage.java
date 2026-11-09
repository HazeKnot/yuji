package com.yuji.profile.service;

import com.yuji.common.BusinessException;
import com.yuji.common.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储：文件落在 {@code yuji.storage.local-dir} 下，目录结构与 objectKey 一致。
 */
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(String objectKey, InputStream in, long size, String contentType) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件保存失败，请稍后重试");
        }
    }

    @Override
    public InputStream open(String objectKey) {
        Path target = resolve(objectKey);
        if (!Files.exists(target)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告文件不存在或已被删除");
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件读取失败，请稍后重试");
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "文件删除失败，请稍后重试");
        }
    }

    @Override
    public String type() {
        return "LOCAL";
    }

    /**
     * 防目录穿越：key 由服务端生成，但解析后仍必须落在 root 内（纵深防御）。
     */
    Path resolve(String objectKey) {
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的文件路径");
        }
        return target;
    }
}
