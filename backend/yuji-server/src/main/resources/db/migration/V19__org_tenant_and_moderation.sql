-- 二期迭代 2：机构租户化 + AI 社区风控
-- 商业模式调整：机构不再只是"平台上的黄页"，而是租户——用平台管理自己的用户。
-- 机构的用户边界 = 向该机构提交过预约意向的用户（partner_lead.user_id 去重）。

-- 1. 机构账号绑定到合作机构（ORG 角色账号必须归属一个 partner_org 才能进工作台）
ALTER TABLE `user`
    ADD COLUMN org_id BIGINT NULL COMMENT '机构账号绑定的合作机构（partner_org.id）' AFTER consent_version;

UPDATE `user` SET org_id = 1 WHERE role = 'ORG';

-- 2. AI 风控结果落在帖子上，机构端告警页直接用
ALTER TABLE community_post
    ADD COLUMN risk_level VARCHAR(10) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/WATCH/ALERT/BLOCK' AFTER status,
    ADD COLUMN risk_score INT NOT NULL DEFAULT 0 AFTER risk_level,
    ADD COLUMN risk_tags VARCHAR(200) NULL COMMENT '命中的风险词，逗号分隔' AFTER risk_score;

-- 3. 多机构演示数据：给另外两家机构补线索，用于演示租户隔离（机构 A 看不到机构 B 的用户）
INSERT INTO partner_lead (org_id, user_id, name, phone, need, preferred_date, status, note)
VALUES
(2, NULL, '周女士', '13711112222', '28 天月子套餐咨询，想看房间', '2026-12-01', 'CONTACTED', '已加微信发资料'),
(2, NULL, '陈女士', '13733334444', '产后修复 10 次卡价格', NULL, 'NEW', NULL),
(3, NULL, '王女士', '13755556666', '两癌筛查预约', '2026-11-20', 'BOOKED', '已短信确认'),
(4, NULL, '郑女士', '13777778888', '盆底肌评估 + 训练', '2026-11-25', 'NEW', NULL);
