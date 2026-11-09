package com.yuji.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户的一次产检项（由模板按孕周生成，可标记状态/上传报告）。
 */
@Data
@TableName("checkup_item")
public class CheckupItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String code;

    private String name;

    /** 建议检查孕周 */
    private Integer suggestedWeek;

    /** 建议检查日期 = lmp + suggestedWeek * 7 */
    private LocalDate dueDate;

    /** PLANNED / DONE / SKIPPED */
    private String status;

    private String reportUrl;

    private String note;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
