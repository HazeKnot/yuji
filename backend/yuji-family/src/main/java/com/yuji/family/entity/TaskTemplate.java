package com.yuji.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 任务包模板（如"产后第 1 周"），明细见 {@link TaskTemplateItem}。
 */
@Data
@TableName("task_template")
public class TaskTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String stage;

    private String name;

    private String description;
}
