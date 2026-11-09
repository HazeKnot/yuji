package com.yuji.wellness.mental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emotion_checkin")
public class EmotionCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 1 很差 - 5 很好 */
    private Integer mood;

    private String tags;

    private String note;

    private LocalDateTime createdAt;
}
