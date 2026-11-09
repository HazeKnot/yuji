-- 她社区：帖子 / 评论 / 点赞
-- 设计要点：author_name 落库快照，匿名帖不反查用户；点赞用唯一键防重复。
CREATE TABLE community_post (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    author_name   VARCHAR(50)   NOT NULL COMMENT '发帖时昵称快照',
    category      VARCHAR(20)   NOT NULL COMMENT 'EXPERIENCE/SECRET/CONFINEMENT/PARENTING/MOOD',
    title         VARCHAR(80)   NOT NULL,
    content       TEXT          NOT NULL,
    anonymous     TINYINT       NOT NULL DEFAULT 0,
    like_count    INT           NOT NULL DEFAULT 0,
    comment_count INT           NOT NULL DEFAULT 0,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/PENDING/HIDDEN',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME      NULL,
    KEY idx_category_status (category, status, created_at),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区帖子';

CREATE TABLE community_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    author_name VARCHAR(50) NOT NULL,
    content     VARCHAR(1000) NOT NULL,
    anonymous   TINYINT     NOT NULL DEFAULT 0,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  DATETIME    NULL,
    KEY idx_post (post_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区评论';

CREATE TABLE community_like (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '社区点赞';

-- 种子：8 篇帖子 + 6 条评论（user_id=0 表示官方运营内容，不关联真实账号）
INSERT INTO community_post (user_id, author_name, category, title, content, anonymous, like_count, comment_count, status)
VALUES
(0, '育己小助手', 'EXPERIENCE', '第一次做盆底评估，原来不用害羞', '产后 42 天复查时医生问我要不要做盆底评估，我犹豫了很久。实际过程就是躺下、配合收缩几次，全程不到十分钟。\n\n结果说我轻度压力性尿失禁，安排了 10 次训练。现在咳嗽基本不漏了。想跟还在犹豫的姐妹说：这是很常规的检查，医生一天要看很多人，真的不用不好意思。', 0, 42, 2, 'PUBLISHED'),
(0, '匿名姐妹', 'SECRET', '分泌物反复不好，最后查出来是这个', '连续几个月反反复复，自己买洗液越洗越糟。后来去妇科做了白带常规，是真菌感染，规范用药两周就好了。\n\n写出来是想说：不要自己乱用药，也不要因为羞耻拖着。检查真的很快。', 1, 36, 1, 'PUBLISHED'),
(0, '小满妈妈', 'CONFINEMENT', '月子里最值得提前准备的三件事', '1. 把"谁来做饭、谁夜里起床"提前写清楚，别等吵起来再分工。\n2. 提前跟家人说好：访客要限量，进门先洗手。\n3. 给自己留一个可以关门的房间，哪怕每天只有半小时。\n\n月子不是"熬过去"，是可以提前设计的。', 0, 58, 2, 'PUBLISHED'),
(0, '安安妈', 'PARENTING', '辅食添加第一周，我踩过的坑', '一开始追求"每天不重样"，结果娃不吃、我焦虑。后来改成一种食材连吃三天观察过敏，反而顺了。\n\n还有一点：一岁前不加盐和糖，老人觉得没味道，我就让他们尝一口纯南瓜泥，真的甜。', 0, 27, 0, 'PUBLISHED'),
(0, '匿名姐妹', 'MOOD', '产后第 20 天，我在卫生间哭了很久', '不是不喜欢宝宝，就是觉得自己什么都做不好。夜里喂奶的时候特别安静，安静到害怕。\n\n后来在育己做了情绪打卡，提示我可以找人聊聊。我告诉了老公，他没有说"你想多了"，只是把夜里那次喂奶接过去了。现在好多了。\n\n如果你也在这样的夜里，先别一个人扛。', 1, 96, 1, 'PUBLISHED'),
(0, '林小满', 'EXPERIENCE', '痛经十几年，终于去看了医生', '以前总觉得痛经是正常的，每个月靠止痛药硬扛。今年体检顺便说了句，医生让我做了超声，发现有子宫内膜异位症。\n\n不是要吓大家，而是想说：如果痛经逐年加重、经期外也痛，值得去查一次。知道原因之后，反而没那么慌了。', 0, 44, 1, 'PUBLISHED'),
(0, '育己小助手', 'EXPERIENCE', '关于"难以启齿"，我们想说的', '在这里，你可以匿名问任何关于月经、分泌物、乳房、盆底、避孕的问题。\n\n我们不做诊断，但会告诉你：这种情况通常需要看哪个科、去之前可以准备什么、哪些信号需要尽快就医。\n\n身体的困惑不需要羞耻，它只是需要被认真对待。', 0, 63, 0, 'PUBLISHED'),
(0, '禾禾妈', 'PARENTING', '两岁半戒尿布，我们用了三周', '没有强行把尿，先让他看大人上厕所，买了一个小马桶放在客厅。第一周只坐不脱裤子，第二周开始定时坐，第三周白天就不用尿布了。\n\n夜里还在用，我们不着急。每个孩子节奏不一样，别被"别人家孩子"推着走。', 0, 31, 0, 'PUBLISHED');

INSERT INTO community_comment (post_id, user_id, author_name, content, anonymous)
VALUES
(1, 0, '匿名姐妹', '请问挂的是妇科还是产后康复科？我也想去但不知道挂哪个。', 1),
(1, 0, '育己小助手', '一般先挂妇科或产后康复门诊，42 天复查时可以直接问医生。不同医院科室名称略有差异，挂号页搜"盆底"通常能找到。', 0),
(2, 0, '匿名姐妹', '看到这条去查了，确实是真菌感染，已经在用药了。谢谢你把这件事写出来。', 1),
(3, 0, '匿名姐妹', '第 3 条太重要了，我当时就是没有可以关门的房间。', 1),
(3, 0, '小满妈妈', '抱抱，下次提前跟家人说，哪怕每天半小时也好。', 0),
(5, 0, '匿名姐妹', '我痛经也逐年加重，一直不敢去查，这周去挂号。', 1);
