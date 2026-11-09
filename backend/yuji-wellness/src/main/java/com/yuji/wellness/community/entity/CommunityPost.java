package com.yuji.wellness.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("community_post")
public class CommunityPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 昵称快照：匿名帖渲染时替换为"匿名姐妹"，不回查用户 */
    private String authorName;

    /** EXPERIENCE / SECRET / CONFINEMENT / PARENTING / MOOD */
    private String category;

    private String title;

    private String content;

    private Boolean anonymous;

    private Integer likeCount;

    private Integer commentCount;

    /** PUBLISHED / PENDING / HIDDEN */
    private String status;

    /** AI 风控级别：NONE/WATCH/ALERT/BLOCK */
    private String riskLevel;

    private Integer riskScore;

    /** 命中的风险词，逗号分隔 */
    private String riskTags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
