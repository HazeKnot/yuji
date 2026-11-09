package com.yuji.wellness.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("care_task")
public class CareTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long userId;

    private LocalDate dueDate;

    private Integer dayIndex;

    private String category;

    private String title;

    private String detail;

    /** PENDING / DONE / SKIPPED */
    private String status;

    private Integer sortNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
