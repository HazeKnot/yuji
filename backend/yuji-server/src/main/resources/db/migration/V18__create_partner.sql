-- B 端合作机构与服务线索
-- 商业模式：妇幼保健院/月子中心入驻，平台提供内容、筛查工单与预约线索，不参与医疗分成。
CREATE TABLE partner_org (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(30)  NOT NULL COMMENT 'HOSPITAL/MATERNITY/POSTPARTUM/COMMUNITY',
    city          VARCHAR(40)  NOT NULL,
    district      VARCHAR(40)  NULL,
    address       VARCHAR(200) NULL,
    contact_name  VARCHAR(40)  NULL,
    contact_phone VARCHAR(30)  NULL,
    intro         VARCHAR(500) NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/PENDING',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME     NULL,
    KEY idx_city_type (city, type, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合作机构';

CREATE TABLE partner_service (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id      BIGINT       NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    price_text  VARCHAR(60)  NULL COMMENT '展示价或"按医院收费标准"',
    description VARCHAR(300) NULL,
    sort_no     INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_org (org_id, sort_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合作机构服务包';

CREATE TABLE partner_lead (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id         BIGINT       NOT NULL,
    user_id        BIGINT       NULL,
    name           VARCHAR(40)  NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    need           VARCHAR(200) NOT NULL,
    preferred_date DATE         NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'NEW' COMMENT 'NEW/CONTACTED/BOOKED/CLOSED',
    note           VARCHAR(300) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_org_status (org_id, status),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合作机构预约线索';

INSERT INTO partner_org (name, type, city, district, address, contact_name, contact_phone, intro, status)
VALUES
('杭州市妇幼保健院', 'HOSPITAL', '杭州', '上城区', '鲲鹏路 369 号', '妇幼合作办', '0571-56005000', '三级甲等妇幼保健院，设有产后康复、盆底诊治、更年期门诊与心理筛查门诊。', 'ACTIVE'),
('馨月母婴护理中心', 'MATERNITY', '杭州', '西湖区', '文三路 188 号', '顾问小周', '0571-88001234', '28/42 天月子套餐，母婴同室，配备哺乳顾问与产后修复师。', 'ACTIVE'),
('拱墅区妇幼保健计划生育服务中心', 'COMMUNITY', '杭州', '拱墅区', '湖墅南路 200 号', '社区保健科', '0571-88002345', '提供免费叶酸、两癌筛查、孕产期随访与更年期健康管理。', 'ACTIVE'),
('她悦产后康复中心', 'POSTPARTUM', '杭州', '滨江区', '江南大道 500 号', '康复顾问', '0571-86005678', '盆底肌评估与训练、腹直肌修复、产后体态与心理疏导。', 'ACTIVE');

INSERT INTO partner_service (org_id, name, price_text, description, sort_no)
VALUES
(1, '产后 42 天复查', '按医院收费标准', '伤口、子宫复旧、盆底功能与情绪筛查。', 1),
(1, '盆底功能评估', '按医院收费标准', '压力性尿失禁、盆腔器官脱垂初筛。', 2),
(1, '更年期专科门诊', '按医院收费标准', '潮热、失眠、情绪波动的系统评估。', 3),
(2, '28 天月子套餐', '面议', '母婴同室、三餐三点、哺乳指导、每日查房。', 1),
(2, '产后修复 10 次卡', '面议', '腹直肌与盆底基础训练，按评估结果定制。', 2),
(3, '免费叶酸领取', '免费', '备孕及孕早期人群，凭身份证领取。', 1),
(3, '两癌筛查预约', '按政策', '宫颈癌与乳腺癌筛查，适龄女性定期参加。', 2),
(4, '盆底肌评估 + 训练', '面议', '生物反馈评估，提供居家训练方案。', 1),
(4, '产后心理疏导', '面议', '持证心理咨询师一对一，可与盆底康复联合安排。', 2);
