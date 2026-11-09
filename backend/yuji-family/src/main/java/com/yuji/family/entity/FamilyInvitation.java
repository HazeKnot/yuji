package com.yuji.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次性邀请码：24 小时有效，用完即作废（usedBy 非空）。
 */
@Data
@TableName("family_invitation")
public class FamilyInvitation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private Long familyId;

    private Long createdBy;

    private LocalDateTime expiresAt;

    private Long usedBy;

    private LocalDateTime usedAt;

    private LocalDateTime createdAt;
}
