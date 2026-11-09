-- 妈妈健康档案
CREATE TABLE profile (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    nickname        VARCHAR(50),
    lmp_date        DATE         NULL COMMENT '末次月经第一天',
    edd             DATE         NULL COMMENT '预产期 = lmp + 280 天',
    concerns        VARCHAR(500) NULL COMMENT '关注点 JSON 数组',
    consent_version VARCHAR(20),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '妈妈健康档案';
