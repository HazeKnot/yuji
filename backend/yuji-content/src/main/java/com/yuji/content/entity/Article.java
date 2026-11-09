package com.yuji.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一篇孕产期知识内容。
 *
 * <p>{@code minWeek}/{@code maxWeek} 是"适用孕周区间"——用区间而不是"发布日期"，
 * 才能保证 12 周的人不会收到待产包清单。
 */
@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 稳定业务编码（灰度、埋点用） */
    private String code;

    private String title;

    /** NUTRITION / CHECKUP / BODY / MIND / PREPARE */
    private String category;

    private Integer minWeek;

    private Integer maxWeek;

    private String summary;

    private String body;

    /** 来源标注：页面直接展示，内容必须可溯源 */
    private String source;

    /** 审核人（合规留痕） */
    private String reviewer;

    private Boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
