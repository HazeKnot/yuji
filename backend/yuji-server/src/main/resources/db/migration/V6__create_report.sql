-- 检查报告：只存元数据与对象键，文件本体在对象存储（本地磁盘 / MinIO 可切换）
CREATE TABLE report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    checkup_item_id BIGINT       NULL COMMENT '关联产检项（可为空：自由上传）',
    file_name       VARCHAR(200) NOT NULL COMMENT '原始文件名（仅用于展示）',
    object_key      VARCHAR(300) NOT NULL COMMENT '存储对象键，如 reports/12/uuid.pdf',
    content_type    VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    storage         VARCHAR(20)  NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/MINIO',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL,
    KEY idx_user (user_id),
    KEY idx_checkup (checkup_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '检查报告';
