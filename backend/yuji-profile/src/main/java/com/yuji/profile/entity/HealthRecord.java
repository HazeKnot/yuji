package com.yuji.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次指标记录。type 决定 primaryValue/secondaryValue 的语义：
 * 体重 kg / 血压（收缩压+舒张压）/ 血糖 mmol/L / 胎动 次。
 */
@Data
@TableName("health_record")
public class HealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** WEIGHT / BLOOD_PRESSURE / BLOOD_GLUCOSE / FETAL_MOVEMENT */
    private String type;

    private BigDecimal primaryValue;

    private BigDecimal secondaryValue;

    private LocalDateTime measuredAt;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
