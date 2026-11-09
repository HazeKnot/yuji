package com.yuji.wellness.partner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("partner_org")
public class PartnerOrg {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** HOSPITAL / MATERNITY / POSTPARTUM / COMMUNITY */
    private String type;

    private String city;

    private String district;

    private String address;

    private String contactName;

    private String contactPhone;

    private String intro;

    /** ACTIVE / PAUSED / PENDING */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
