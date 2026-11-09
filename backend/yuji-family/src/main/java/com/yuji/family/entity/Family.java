package com.yuji.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭：owner 是建档的妈妈，成员通过邀请码加入。
 */
@Data
@TableName("family")
public class Family {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerUserId;

    private String name;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
