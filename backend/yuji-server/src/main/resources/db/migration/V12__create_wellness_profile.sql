-- 女性全周期档案与周期记录
-- 设计要点：孕产档案（profile）继续服务孕期人群；wellness_profile 承载经期/备孕/产后/更年期等非孕期场景，
-- 两者按 user_id 一对一，不合并表，避免把"孕周"这一强约束塞进所有女性用户。
CREATE TABLE wellness_profile (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    life_stage       VARCHAR(20)  NOT NULL DEFAULT 'CYCLE' COMMENT 'CYCLE/PREPARE/PREGNANT/POSTPARTUM/PARENTING/MENOPAUSE',
    last_period_date DATE         NULL COMMENT '最近一次月经第一天',
    cycle_length     INT          NOT NULL DEFAULT 28 COMMENT '平均周期天数（21-45）',
    period_length    INT          NOT NULL DEFAULT 5 COMMENT '平均经期天数（2-10）',
    baby_birth_date  DATE         NULL COMMENT '宝宝出生日期（育儿内容按月龄命中）',
    delivery_date    DATE         NULL COMMENT '分娩日期（月子计划起点）',
    delivery_mode    VARCHAR(20)  NULL COMMENT 'NATURAL 顺产 / CESAREAN 剖宫产',
    concerns         VARCHAR(500) NULL COMMENT '关注点 JSON 数组',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at       DATETIME     NULL,
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '女性全周期健康档案';

CREATE TABLE cycle_record (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    start_date DATE         NOT NULL COMMENT '本次月经第一天',
    end_date   DATE         NULL COMMENT '本次月经结束日',
    flow       VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'LIGHT/NORMAL/HEAVY',
    symptoms   VARCHAR(300) NULL COMMENT '症状标签，逗号分隔：痛经/腰酸/乳房胀/情绪低落/头痛/水肿',
    mood       VARCHAR(20)  NULL COMMENT '经期心情：CALM/LOW/IRRITABLE/ANXIOUS',
    note       VARCHAR(300) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME     NULL,
    KEY idx_user_start (user_id, start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '月经周期记录';

-- 演示账号的全周期档案（org001 不建；只给普通妈妈演示用，登录后可用 onboarding 覆盖）
-- 不写种子：真实用户数据由用户自己产生，避免演示数据污染个人健康记录。
