package com.yuji.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表。角色：MOM / FAMILY / ORG / VOLUNTEER / ADMIN。
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String passwordHash;

    private String nickname;

    private String role;

    private String status;

    /** 同意版本号（健康数据合规：记录用户同意过的隐私政策版本） */
    private String consentVersion;

    /** 机构账号绑定的合作机构（partner_org.id），MOM/FAMILY 用户为 null */
    private Long orgId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
