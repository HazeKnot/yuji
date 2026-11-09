-- 指标记录：单表多类型（type 驱动语义），避免体重/血压/血糖/胎动各开一张表
CREATE TABLE health_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    type            VARCHAR(20)   NOT NULL COMMENT 'WEIGHT/BLOOD_PRESSURE/BLOOD_GLUCOSE/FETAL_MOVEMENT',
    primary_value   DECIMAL(6, 2) NOT NULL COMMENT '主值：体重kg / 收缩压mmHg / 血糖mmol/L / 胎动次',
    secondary_value DECIMAL(6, 2) NULL COMMENT '次值：舒张压（仅血压使用）',
    measured_at     DATETIME      NOT NULL COMMENT '测量时间（用户可回填）',
    note            VARCHAR(200)  NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      DATETIME      NULL,
    KEY idx_user_type_time (user_id, type, measured_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户指标记录';
