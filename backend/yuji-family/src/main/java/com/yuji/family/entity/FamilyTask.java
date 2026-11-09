package com.yuji.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 协同任务：由妈妈按模板下发，指派给某个家庭成员，家属打卡后妈妈端实时可见。
 */
@Data
@TableName("family_task")
public class FamilyTask {

    public static final String PENDING = "PENDING";
    public static final String DOING = "DOING";
    public static final String DONE = "DONE";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long creatorUserId;

    private Long assigneeUserId;

    private String templateCode;

    private String title;

    private String detail;

    private LocalDate dueDate;

    private String status;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
