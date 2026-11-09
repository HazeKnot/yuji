-- 月子调理与育儿教育
-- 设计要点：月子任务是"按产后天数的可执行清单"，不是医疗处方；42 天节点对应产后复查。
CREATE TABLE care_task_template (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_offset  INT          NOT NULL COMMENT '相对分娩日的第几天',
    category    VARCHAR(20)  NOT NULL COMMENT 'DIET/RECOVERY/REST/FEEDING/MOOD/CHECKUP',
    title       VARCHAR(80)  NOT NULL,
    detail      VARCHAR(500) NOT NULL,
    sort_no     INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_day_sort (day_offset, sort_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '月子调理任务模板';

INSERT INTO care_task_template (day_offset, category, title, detail, sort_no)
VALUES
(1, 'DIET', '产后第一餐：温热、清淡、易消化', '小米粥、烂面条、蒸蛋都可以。先别急着喝浓汤，等排气/肠胃恢复后再逐步增加。剖宫产按医嘱禁食时间。', 1),
(1, 'RECOVERY', '尽早下床活动', '顺产一般 6-12 小时内可缓慢下床；剖宫产按医生安排。第一次下床先坐 1 分钟再站，防止体位性低血压。', 2),
(1, 'FEEDING', '早接触、早吸吮、按需哺乳', '产后 1 小时内开始皮肤接触和吸吮有助于开奶。含接姿势比"有没有奶"更重要，疼说明含接需要调整。', 3),
(2, 'RECOVERY', '观察恶露颜色与量', '正常恶露由红转淡、量逐渐减少。若突然增多、有大血块、异味或发热，及时联系医院。', 1),
(3, 'DIET', '补充优质蛋白与铁', '鸡蛋、鱼、瘦肉、豆制品每天安排；搭配深绿色蔬菜和富含维生素 C 的水果帮助铁吸收。', 1),
(5, 'MOOD', '做一次 30 秒情绪打卡', '不用等到"撑不住"才记录。连续低落、对什么都提不起兴趣时，可以到「关怀 - 心理康复」做一次量表筛查。', 1),
(7, 'RECOVERY', '伤口/会阴护理检查', '每天观察切口有无红肿、渗液、裂开；保持清洁干燥。会阴伤口疼痛明显加重或坐立困难，及时就诊。', 1),
(10, 'DIET', '增加蔬菜与水分，预防便秘', '每天 1.5-2L 水，蔬菜一斤左右，主食里加粗粮。便秘时不要长期用力屏气，会影响盆底。', 1),
(14, 'RECOVERY', '开始温和的盆底唤醒', '先做腹式呼吸和凯格尔：收缩 3-5 秒、放松同样时间，每天 3 组，每组 10 次。剖宫产或伤口未愈合先咨询医生。', 1),
(21, 'MOOD', '和家人做一次分工复盘', '把夜里喂奶、做饭、访客接待写成清单，明确谁负责。说不出口的期待，最后都会变成委屈。', 1),
(28, 'REST', '逐步恢复日常活动', '从每天 20 分钟散步开始，避免提重物和长时间站立。出汗多、头晕心慌时停下来休息。', 1),
(35, 'CONTRACEPTION', '了解产后避孕方式', '哺乳期不等于不会怀孕。避孕套、宫内节育器、皮下埋植等都可在医生评估后选择；哺乳期口服避孕药需咨询医生。', 1),
(42, 'CHECKUP', '产后 42 天复查', '复查项目通常包括伤口、子宫复旧、盆底功能、血压体重，以及情绪筛查。去之前把想问的问题写下来，别不好意思开口。', 1);

CREATE TABLE care_plan (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    delivery_date DATE        NOT NULL,
    delivery_mode VARCHAR(20) NOT NULL DEFAULT 'NATURAL' COMMENT 'NATURAL/CESAREAN',
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME    NULL,
    KEY idx_user (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '月子调理计划';

CREATE TABLE care_task (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    due_date   DATE        NOT NULL,
    day_index  INT         NOT NULL COMMENT '产后第几天',
    category   VARCHAR(20) NOT NULL,
    title      VARCHAR(80) NOT NULL,
    detail     VARCHAR(500) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/SKIPPED',
    sort_no    INT         NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_plan_date (plan_id, due_date),
    KEY idx_user_date (user_id, due_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '月子调理任务实例';

CREATE TABLE parenting_course (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(40)  NOT NULL,
    title      VARCHAR(80)  NOT NULL,
    summary    VARCHAR(200) NOT NULL,
    min_month  INT          NOT NULL COMMENT '适用月龄下限（含）',
    max_month  INT          NOT NULL COMMENT '适用月龄上限（含）',
    min_week   INT          NOT NULL DEFAULT 0 COMMENT '产前课程适用孕周下限，0 表示不限',
    source     VARCHAR(200) NOT NULL,
    reviewer   VARCHAR(50)  NULL,
    published  TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME     NULL,
    UNIQUE KEY uk_code (code),
    KEY idx_month (min_month, max_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '育儿教育课程';

CREATE TABLE parenting_lesson (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT       NOT NULL,
    title        VARCHAR(100) NOT NULL,
    content      TEXT         NOT NULL,
    duration_min INT          NOT NULL DEFAULT 5,
    sort_no      INT          NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   DATETIME     NULL,
    KEY idx_course (course_id, sort_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '育儿课程课时';

INSERT INTO parenting_course (code, title, summary, min_month, max_month, min_week, source, reviewer)
VALUES
('NEWBORN_BASIC', '新生儿护理基础', '脐带、黄疸、大小便、体温，新手爸妈最先要会的几件事。', 0, 3, 28, '《新生儿保健工作规范》；WHO 新生儿早期基本保健', '儿科内容组'),
('BREASTFEEDING', '母乳喂养与追奶', '含接、频率、涨奶与堵奶，以及什么时候需要找哺乳顾问。', 0, 6, 24, '《母乳喂养促进行动计划（2021-2025）》', '产科内容组'),
('SLEEP', '睡眠引导：先建立作息，再谈自主入睡', '不同月龄的睡眠总量、睡前程序、夜醒处理。', 2, 12, 0, '《0-5 岁儿童睡眠卫生指南》', '儿保内容组'),
('SOLID_FOOD', '辅食添加第一课', '满 6 月龄开始，一种一种加，观察过敏。', 5, 12, 0, '《中国居民膳食指南（2022）》婴幼儿喂养指南', '营养内容组'),
('LANGUAGE', '语言与情绪：一岁后的关键窗口', '多说、多回应，比早教课更重要。', 12, 24, 0, '《儿童早期发展指南》；WHO 养育照护框架', '儿保内容组'),
('TOILET', '如厕训练与边界感', '两岁后开始，不强迫、不比较。', 24, 36, 0, '《儿童保健学》；美国儿科学会如厕训练建议', '儿保内容组');

INSERT INTO parenting_lesson (course_id, title, content, duration_min, sort_no)
VALUES
(1, '脐带护理：保持干燥就够了', '脐带残端通常在 1-2 周脱落。日常用清水清洁、保持干燥，尿布前端向下折避免摩擦。\n\n出现红肿、渗液、异味或超过 4 周未脱落，及时就医。不要自行涂抹药粉。', 4, 1),
(1, '黄疸：什么时候需要去医院', '多数新生儿在出生后 2-3 天出现生理性黄疸，7-10 天逐渐消退。\n\n需要尽快就医的信号：出生 24 小时内出现黄疸、黄疸蔓延到手脚心、宝宝嗜睡吃奶差、黄疸退而复现。\n\n晒太阳不能替代经皮胆红素监测。', 5, 2),
(2, '含接姿势：不疼才是对的', '宝宝嘴巴张大、含住大部分乳晕，下巴贴住乳房，吸吮时脸颊鼓起。\n\n乳头疼痛、破损，先调整姿势，不要硬扛。持续疼痛或反复堵奶，建议找哺乳顾问或产科门诊。', 6, 1),
(2, '涨奶与堵奶：先冷敷还是热敷', '喂奶前可以温敷 3-5 分钟帮助奶阵，喂奶后冷敷缓解肿胀。\n\n堵奶时继续按需哺乳，从硬块方向朝乳头轻柔按摩。出现发烧、乳房红肿热痛，可能是乳腺炎，需要就医。', 6, 2),
(3, '0-3 月：先建立昼夜节律', '白天拉开窗帘、正常生活噪音；夜里喂奶保持昏暗安静。\n\n这个月龄不需要"训练自主入睡"，及时回应不会惯坏孩子。', 5, 1),
(3, '4-6 月：睡前程序固定下来', '洗澡-换睡衣-喂奶-关灯-放床，每天顺序一致。\n\n夜醒先观察 1-2 分钟再回应，但不必机械等待。睡眠引导的前提是白天充足的安全感。', 5, 2),
(4, '第一口辅食：铁强化米粉或肉泥', '满 6 月龄、能稳稳抬头、对食物感兴趣时开始。\n\n第一口优先选富含铁的食物。一次只加一种新食材，连续 3 天观察皮疹、呕吐、腹泻。', 6, 1),
(4, '一岁前不加盐、不加糖、不喝鲜奶', '宝宝的肾脏还处理不了额外的盐。天然食物本身有味道，南瓜、红薯、玉米都有甜味。\n\n蜂蜜一岁前禁用，有肉毒杆菌风险。', 4, 2),
(5, '多说：把日常变成语言输入', '换尿布时说出你在做什么，读书时指图命名，孩子发声时回应他。\n\n屏幕替代不了真人互动。18 月龄前尽量避免独自看屏幕。', 5, 1),
(5, '情绪风暴：先接住，再讲道理', '一岁后的哭闹多是能力跟不上愿望。先蹲下、命名情绪："你很想要那个，拿不到很难过。"\n\n情绪平复后再给边界。打骂会让情绪更难被调节。', 6, 2),
(6, '什么时候开始如厕训练', '多数孩子在 18-30 月龄出现准备信号：能保持尿布干燥 2 小时、能表达要上厕所、对马桶感兴趣。\n\n不强迫、不比较，白天先脱尿布，夜里顺其自然。', 5, 1),
(6, '倒退是正常的', '刚学会如厕后又频繁尿裤子，通常不是故意。换新环境、家里添弟弟妹妹、生病都可能引起倒退。\n\n平静处理、减少压力，比反复提醒更有效。', 4, 2);
