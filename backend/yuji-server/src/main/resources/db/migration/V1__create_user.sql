-- 用户表（一期：手机号 + 密码；家属/机构角色为后续模块预留）
CREATE TABLE `user` (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone           VARCHAR(20)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nickname        VARCHAR(50),
    role            VARCHAR(20)  NOT NULL DEFAULT 'MOM',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    consent_version VARCHAR(20),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    UNIQUE KEY uk_phone (phone),
    KEY idx_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';
