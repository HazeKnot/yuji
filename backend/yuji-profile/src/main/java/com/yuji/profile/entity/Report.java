package com.yuji.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查报告元数据；文件本体存在对象存储，objectKey 是唯一线索。
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 关联产检项，可为空（自由上传） */
    private Long checkupItemId;

    private String fileName;

    private String objectKey;

    private String contentType;

    private Long sizeBytes;

    /** LOCAL / MINIO */
    private String storage;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
