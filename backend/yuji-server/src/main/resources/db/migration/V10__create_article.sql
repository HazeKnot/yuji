-- 知识内容：按孕周区间命中，不按"文章发布时间"排序
-- 设计要点：min_week/max_week 让同一篇内容可以覆盖一段孕周，避免给 12 周的人推 30 周的待产包
CREATE TABLE article (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL COMMENT '稳定业务编码，便于内容灰度与埋点',
    title      VARCHAR(120) NOT NULL,
    category   VARCHAR(20)  NOT NULL COMMENT 'NUTRITION/CHECKUP/BODY/MIND/PREPARE',
    min_week   INT          NOT NULL COMMENT '适用孕周下限（含）',
    max_week   INT          NOT NULL COMMENT '适用孕周上限（含）',
    summary    VARCHAR(200) NOT NULL COMMENT '列表摘要，一句话说清这篇讲什么',
    body       TEXT         NOT NULL,
    source     VARCHAR(200) NOT NULL COMMENT '来源标注（内容可溯源，页面直接展示）',
    reviewer   VARCHAR(50)  NULL COMMENT '内容审核人（合规留痕）',
    published  TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME     NULL,
    UNIQUE KEY uk_code (code),
    KEY idx_week (min_week, max_week)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '孕产期知识内容';
