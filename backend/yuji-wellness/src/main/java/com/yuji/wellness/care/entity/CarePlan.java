package com.yuji.wellness.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("care_plan")
public class CarePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate deliveryDate;

    /** NATURAL / CESAREAN */
    private String deliveryMode;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
