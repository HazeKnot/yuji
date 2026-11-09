package com.yuji.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 妈妈健康档案（一期只做孕期；产后/更年期字段为远期预留）。
 */
@Data
@TableName("profile")
public class Profile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String nickname;

    /** 末次月经第一天（与 edd 二选一，服务层换算） */
    private LocalDate lmpDate;

    /** 预产期 = lmp + 280 天 */
    private LocalDate edd;

    /** 关注点，JSON 数组字符串，如 ["产检提醒","情绪状态"] */
    private String concerns;

    private String consentVersion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
