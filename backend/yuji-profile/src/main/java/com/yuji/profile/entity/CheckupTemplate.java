package com.yuji.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产检时间表模板（种子数据来源于《孕前和孕期保健指南（2018）》，具体以产检医院安排为准）。
 */
@Data
@TableName("checkup_template")
public class CheckupTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private Integer suggestedWeek;

    /** 建议孕周窗口文案，如"孕 11–13+6 周" */
    private String windowText;

    private String description;

    /** 来源标注（合规要求：科普内容必须可溯源） */
    private String source;

    private LocalDateTime createdAt;
}
