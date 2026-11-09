package com.yuji.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭成员 + 字段级授权。
 *
 * <p>{@code visibleFields} 为逗号分隔的指标类型（如 {@code WEIGHT,BLOOD_PRESSURE}），空串表示
 * 只见任务与状态摘要。情绪记录、量表得分不在可授权枚举内，家属永远看不到。
 */
@Data
@TableName("family_member")
public class FamilyMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long userId;

    /** 称呼：配偶 / 母亲 / 月嫂…（仅展示） */
    private String relation;

    private String visibleFields;

    /** ACTIVE / REMOVED */
    private String status;

    private LocalDateTime joinedAt;

    private LocalDateTime updatedAt;
}
