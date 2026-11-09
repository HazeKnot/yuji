package com.yuji.wellness.consult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("consult_session")
public class ConsultSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String topicCode;

    private String title;

    /** GREEN / YELLOW / RED */
    private String riskLevel;

    /** OPEN / REFERRED / CLOSED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
