-- 产后心理康复：情绪打卡 / 量表结果 / 风险工单
-- 合规边界：量表只做筛查提示，不下诊断；黄档人工陪伴，红档转介合作机构与心理热线。
CREATE TABLE emotion_checkin (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    mood       INT          NOT NULL COMMENT '1-5，1 很差 5 很好',
    tags       VARCHAR(200) NULL COMMENT '情绪标签，逗号分隔',
    note       VARCHAR(300) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '情绪打卡';

CREATE TABLE scale_result (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    scale_type VARCHAR(20) NOT NULL COMMENT 'EPDS/GAD7',
    score      INT         NOT NULL,
    level      VARCHAR(10) NOT NULL COMMENT 'GREEN/YELLOW/RED',
    answers    VARCHAR(200) NOT NULL COMMENT '10 题答案，逗号分隔',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '量表筛查结果';

CREATE TABLE risk_ticket (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    source       VARCHAR(30)  NOT NULL COMMENT 'EPDS/CONSULT/EMOTION',
    level        VARCHAR(10)  NOT NULL COMMENT 'YELLOW/RED',
    summary      VARCHAR(300) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/FOLLOWING/CLOSED',
    handler_note VARCHAR(500) NULL,
    org_id       BIGINT       NULL COMMENT '转介的合作机构',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at    DATETIME     NULL,
    KEY idx_status_level (status, level, created_at),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '风险跟进工单';
