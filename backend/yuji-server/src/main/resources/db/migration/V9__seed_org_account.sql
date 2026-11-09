-- W5：机构端账号（看板只做聚合统计，不返回任何个人明细）
-- 演示账号：org001 / org123456，上线前必须改密码或关闭该账号
INSERT INTO `user` (phone, password_hash, nickname, role, status, consent_version)
VALUES ('org001',
        '$2a$10$ZWsXc.ECqjRjd7Cjii8m0uDwS6RMdvg/TsnxyiAv1wC2bjwB.YLv.',
        '示范妇幼保健院',
        'ORG',
        'ACTIVE',
        'v1')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), role = VALUES(role);
