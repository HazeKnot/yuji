package com.yuji.wellness.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 女性全周期档案：与孕产档案 profile 按 user_id 一对一，互不替代。
 */
@Data
@TableName("wellness_profile")
public class WellnessProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** CYCLE / PREPARE / PREGNANT / POSTPARTUM / PARENTING / MENOPAUSE */
    private String lifeStage;

    private LocalDate lastPeriodDate;

    private Integer cycleLength;

    private Integer periodLength;

    private LocalDate babyBirthDate;

    private LocalDate deliveryDate;

    /** NATURAL / CESAREAN */
    private String deliveryMode;

    private String concerns;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
