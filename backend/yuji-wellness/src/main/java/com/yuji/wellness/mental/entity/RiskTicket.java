package com.yuji.wellness.mental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_ticket")
public class RiskTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** EPDS / CONSULT / EMOTION */
    private String source;

    /** YELLOW / RED */
    private String level;

    private String summary;

    /** OPEN / FOLLOWING / CLOSED */
    private String status;

    private String handlerNote;

    private Long orgId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;
}
