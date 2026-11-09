package com.yuji.wellness.mental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scale_result")
public class ScaleResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** EPDS / GAD7 */
    private String scaleType;

    private Integer score;

    /** GREEN / YELLOW / RED */
    private String level;

    private String answers;

    private LocalDateTime createdAt;
}
