package com.yuji.wellness.partner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构服务包。类名避开与 PartnerService（业务服务）同名。
 */
@Data
@TableName("partner_service")
public class PartnerServiceItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String name;

    private String priceText;

    private String description;

    private Integer sortNo;

    private LocalDateTime createdAt;
}
