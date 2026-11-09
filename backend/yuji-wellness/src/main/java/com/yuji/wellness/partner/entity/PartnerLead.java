package com.yuji.wellness.partner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("partner_lead")
public class PartnerLead {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private Long userId;

    private String name;

    private String phone;

    private String need;

    private LocalDate preferredDate;

    /** NEW / CONTACTED / BOOKED / CLOSED */
    private String status;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
