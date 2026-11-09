package com.yuji.wellness.consult.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私密健康主题知识卡：命中关键词后返回"可以怎么做 / 何时就医 / 挂什么科"。
 * 它不是诊断模型，答案由人工审核过的内容驱动。
 */
@Data
@TableName("consult_topic")
public class ConsultTopic {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String title;

    private String category;

    private String keywords;

    private String summary;

    private String advice;

    private String redFlags;

    private String careTips;

    private String department;

    private String source;

    private Integer sortNo;

    private LocalDateTime createdAt;
}
