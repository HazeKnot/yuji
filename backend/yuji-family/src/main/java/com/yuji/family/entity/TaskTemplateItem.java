package com.yuji.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 任务包明细：{@code dueOffsetDays} 是相对下发日的截止天数（0 = 当天）。
 */
@Data
@TableName("task_template_item")
public class TaskTemplateItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;

    private String title;

    private String detail;

    private Integer dueOffsetDays;

    private Integer sortNo;
}
