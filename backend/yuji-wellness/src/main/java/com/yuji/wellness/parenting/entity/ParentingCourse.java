package com.yuji.wellness.parenting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("parenting_course")
public class ParentingCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String title;

    private String summary;

    private Integer minMonth;

    private Integer maxMonth;

    private Integer minWeek;

    private String source;

    private String reviewer;

    private Boolean published;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
