package com.yuji.wellness.care.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("care_task_template")
public class CareTaskTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer dayOffset;

    private String category;

    private String title;

    private String detail;

    private Integer sortNo;
}
