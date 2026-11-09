package com.yuji.wellness.parenting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("parenting_lesson")
public class ParentingLesson {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String title;

    private String content;

    private Integer durationMin;

    private Integer sortNo;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;
}
