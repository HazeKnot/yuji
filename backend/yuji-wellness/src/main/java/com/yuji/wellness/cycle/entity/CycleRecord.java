package com.yuji.wellness.cycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("cycle_record")
public class CycleRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate startDate;

    private LocalDate endDate;

    /** LIGHT / NORMAL / HEAVY */
    private String flow;

    /** 逗号分隔：痛经、腰酸、乳房胀、情绪低落、头痛、水肿 */
    private String symptoms;

    /** CALM / LOW / IRRITABLE / ANXIOUS */
    private String mood;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
