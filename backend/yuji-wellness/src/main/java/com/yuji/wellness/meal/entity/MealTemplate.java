package com.yuji.wellness.meal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("meal_template")
public class MealTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** MENSTRUAL / FOLLICULAR / OVULATION / LUTEAL */
    private String phase;

    private Integer dayOffset;

    private String breakfast;

    private String lunch;

    private String dinner;

    private String snack;

    private String nutrients;

    private String ingredients;

    private String tips;

    private LocalDateTime createdAt;
}
