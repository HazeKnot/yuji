-- W4 家属协同：家庭 / 成员（字段级权限）/ 邀请码 / 任务 / 任务包模板
-- 设计要点：权限不写进 user.role（JWT 签发后改角色会与旧 token 不一致），
--          一律以 family_member 表为准，服务端每次按表判定。

CREATE TABLE family (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT      NOT NULL COMMENT '妈妈（建档主体）',
    name          VARCHAR(50) NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_owner (owner_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭';

CREATE TABLE family_member (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id      BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    relation       VARCHAR(20)  NOT NULL DEFAULT 'FAMILY' COMMENT '称呼：配偶/母亲/月嫂…',
    visible_fields VARCHAR(300) NOT NULL DEFAULT '' COMMENT '逗号分隔的授权字段，空=只见任务与状态摘要',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    joined_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id) COMMENT '一期：一个用户只能属于一个家庭',
    KEY idx_family (family_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭成员与字段级授权';

CREATE TABLE family_invitation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(16) NOT NULL,
    family_id  BIGINT      NOT NULL,
    created_by BIGINT      NOT NULL,
    expires_at DATETIME    NOT NULL,
    used_by    BIGINT      NULL,
    used_at    DATETIME    NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_family (family_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '家庭邀请码（一次性）';

CREATE TABLE task_template (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    stage       VARCHAR(30)  NOT NULL COMMENT '适用阶段：孕晚期/产检/产后…',
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(300) NULL,
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务包模板';

CREATE TABLE task_template_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code   VARCHAR(40)  NOT NULL,
    title           VARCHAR(120) NOT NULL,
    detail          VARCHAR(500) NULL COMMENT '步骤化指引',
    due_offset_days INT          NOT NULL DEFAULT 0 COMMENT '相对下发日的截止天数',
    sort_no         INT          NOT NULL DEFAULT 0,
    KEY idx_template (template_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务包模板明细';

CREATE TABLE family_task (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id        BIGINT      NOT NULL,
    creator_user_id  BIGINT      NOT NULL,
    assignee_user_id BIGINT      NOT NULL,
    template_code    VARCHAR(40) NULL,
    title            VARCHAR(120) NOT NULL,
    detail           VARCHAR(500) NULL,
    due_date         DATE        NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DOING/DONE',
    completed_at     DATETIME    NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_family (family_id),
    KEY idx_assignee (assignee_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '协同任务';
