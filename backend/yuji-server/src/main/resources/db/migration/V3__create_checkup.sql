-- 产检项 + 产检时间表模板
CREATE TABLE checkup_template (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    suggested_week INT          NOT NULL COMMENT '建议检查孕周',
    window_text    VARCHAR(100) NULL COMMENT '建议孕周窗口文案',
    description    VARCHAR(500) NULL,
    source         VARCHAR(200) NOT NULL COMMENT '来源标注（内容可溯源）',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '产检时间表模板';

CREATE TABLE checkup_item (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    suggested_week INT          NOT NULL,
    due_date       DATE         NULL COMMENT '建议检查日期 = lmp + suggestedWeek * 7',
    status         VARCHAR(20)  NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/DONE/SKIPPED',
    report_url     VARCHAR(500) NULL,
    note           VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     DATETIME     NULL,
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户产检项';
