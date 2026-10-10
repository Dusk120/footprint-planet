-- ============================================================
-- 足迹星球 · 数据库建表脚本 + 测试数据
-- 对应接口文档 v1.4（40 个接口：30 用户端 + 10 管理端，含角色 ADMIN）
-- 数据库：footprint_planet ｜ 字符集：utf8mb4 ｜ 引擎：InnoDB
--
-- 导入方式（Windows，务必带 utf8mb4，否则中文会乱码）：
--   mysql -u root -p --default-character-set=utf8mb4 < footprint_planet.sql
--
-- 演示账号（密码均为 123456）：
--   admin     管理员
--   zhangsan  张三
--   xiaoA     旅行者小A（社区示例作者）
--   zhuiguangzhe / xinghe  其他社区用户
--
-- 【城市名写法规范】visited_city.city_name 与 note.location_name 必须与前端
-- loadCityPoints() 输出的写法完全一致，否则：
--   1) 热门目的地按 city_name 分组，同一座城会被拆成两行（如「拉萨」+「拉萨市」）；
--   2) 发布笔记时的城市校验（城市名 + 省份都要对上）会对不上。
-- 规则：
--   直辖市 / 特区 → 简称：北京 / 上海 / 天津 / 重庆 / 香港 / 澳门 / 台湾
--                   （与前端 src/data/cities.js 的 MUNI_POINTS 保持一致）
--   其余地级行政区 → 全称：成都市 / 拉萨市 / 大理白族自治州 / 海西蒙古族藏族自治州
--                   （取自前端 public/textures/china-provinces-geo.js 的 name）
-- ============================================================

DROP DATABASE IF EXISTS footprint_planet;
CREATE DATABASE footprint_planet DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE footprint_planet;

-- 删除旧表（按外键依赖逆序）
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS comment_like;
DROP TABLE IF EXISTS note_comment;
DROP TABLE IF EXISTS note_like;
DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS plan;
DROP TABLE IF EXISTS visited_city;
DROP TABLE IF EXISTS user;

-- ------------------------------------------------------------
-- 1. user（用户）
-- ------------------------------------------------------------
CREATE TABLE user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(50)  NOT NULL COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称（可空，可重复；注册不填则自动生成）',
  avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL（可空）',
  motto       VARCHAR(100) DEFAULT NULL COMMENT '个性签名/座右铭',
  total_km    INT          NOT NULL DEFAULT 0 COMMENT '累计里程（km）',
  total_days  INT          NOT NULL DEFAULT 0 COMMENT '累计旅行天数',
  role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER 普通用户 / ADMIN 管理员',
  status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 正常 / 1 禁用',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 演示用户（密码均为 123456）
INSERT INTO user (id, username, password, nickname, avatar, motto, role, total_km, total_days, created_at) VALUES
(1, 'admin',         '$2a$10$vMDo8.gKIfsZIdcAPokj0.VdqkkcUJUi8SgFSTIUv6wkCIP0a.DtO', '管理员',      NULL, '足迹星球的守护者',        'ADMIN', 0,     0,     '2026-07-01 09:00:00'),
(2, 'xiaoA',         '$2a$10$9s400kBaxdwTmN33Uav9a.Lq9/KsgY0IrLC0RLbWUY4/VlAe2Xmz2', '旅行者小A',   NULL, '世界那么大，我想去看看',   'USER',  68500, 156,   '2026-07-03 14:20:00'),
(3, 'zhangsan',      '$2a$10$5lC6apTcnltZs2Ke5aMBTeOYGwV0jumITzZRDN5VBsrqjD1lFmbay', '张三',        NULL, '把日子过成诗',            'USER',  1200,  8,     '2026-08-05 10:30:00'),
(4, 'zhuiguangzhe',  '$2a$10$snjpvdSyhTjBBh1HQvfZpeWs.jnvI0Xvvxjh0.RVbxs4u9nq0aw.i', '追光者',      NULL, '向着光的方向出发',        'USER',  9800,  42,    '2026-07-15 16:45:00'),
(5, 'xinghe',        '$2a$10$vMDo8.gKIfsZIdcAPokj0.VdqkkcUJUi8SgFSTIUv6wkCIP0a.DtO', '星河旅人',    NULL, '把世界走成自己的地图',    'USER',  5600,  21,    '2026-07-28 11:00:00');

-- ------------------------------------------------------------
-- 2. visited_city（点亮城市 / 旅行回忆）
-- ------------------------------------------------------------
CREATE TABLE visited_city (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  user_id    BIGINT      NOT NULL COMMENT '所属用户',
  city_name  VARCHAR(50) NOT NULL COMMENT '城市名',
  province   VARCHAR(50) NOT NULL COMMENT '所在省份',
  adcode     VARCHAR(20) DEFAULT NULL COMMENT '行政区划代码',
  lat        DOUBLE      DEFAULT NULL COMMENT '纬度',
  lng        DOUBLE      DEFAULT NULL COMMENT '经度',
  color      VARCHAR(10) DEFAULT NULL COMMENT '点位颜色（地图）',
  light_date DATE        DEFAULT NULL COMMENT '点亮日期',
  records    JSON        DEFAULT NULL COMMENT '游玩回忆数组',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_city (user_id, city_name),
  KEY idx_user (user_id),
  CONSTRAINT fk_visited_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点亮城市';

-- 城市名规范（必须与前端 loadCityPoints() 输出的写法一致，否则热门目的地按
-- city_name 分组时会把同一座城拆成两行，社区发布笔记时的城市校验也会对不上）：
--   直辖市 / 特区 → 简称（北京 / 上海 / 天津 / 重庆 / 香港 / 澳门 / 台湾，同前端 MUNI_POINTS）
--   其余地级行政区 → 全称（成都市 / 拉萨市 / 大理白族自治州 / 海西蒙古族藏族自治州）
INSERT INTO visited_city (id, user_id, city_name, province, adcode, lat, lng, color, light_date, records, created_at) VALUES
(1, 2, '北京',           '北京市',      '110000', 39.9,   116.4,  '#ef4444', '2026-08-10', JSON_ARRAY(JSON_OBJECT('date','2026-08-01','rating',5,'photos',JSON_ARRAY())), '2026-08-10 20:00:00'),
(2, 2, '成都市',         '四川省',      '510100', 30.57,  104.07, '#f59e0b', '2026-07-20', JSON_ARRAY(JSON_OBJECT('date','2026-07-18','rating',4,'photos',JSON_ARRAY())), '2026-07-20 19:30:00'),
(3, 2, '大理白族自治州', '云南省',      '532900', 25.61,  100.27, '#22c55e', '2026-08-05', JSON_ARRAY(JSON_OBJECT('date','2026-08-02','rating',5,'photos',JSON_ARRAY())), '2026-08-05 21:00:00'),
(4, 3, '武汉市',         '湖北省',      '420100', 30.59,  114.30, '#3b82f6', '2026-08-15', JSON_ARRAY(JSON_OBJECT('date','2026-08-12','rating',4,'photos',JSON_ARRAY())), '2026-08-15 18:40:00'),
(5, 3, '重庆',           '重庆市',      '500000', 29.56,  106.55, '#f97316', '2026-08-20', JSON_ARRAY(JSON_OBJECT('date','2026-08-19','rating',5,'photos',JSON_ARRAY())), '2026-08-20 20:10:00'),
(6, 3, '三亚市',         '海南省',      '460200', 18.25,  109.51, '#06b6d4', '2026-08-25', JSON_ARRAY(JSON_OBJECT('date','2026-08-23','rating',5,'photos',JSON_ARRAY())), '2026-08-25 15:20:00'),
(7, 4, '拉萨市',         '西藏自治区',  '540100', 29.65,  91.10,  '#a855f7', '2026-08-18', JSON_ARRAY(JSON_OBJECT('date','2026-08-15','rating',5,'photos',JSON_ARRAY())), '2026-08-18 12:00:00');

-- ------------------------------------------------------------
-- 3. plan（旅游规划）
-- ------------------------------------------------------------
CREATE TABLE plan (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规划ID',
  user_id    BIGINT       NOT NULL COMMENT '所属用户',
  name       VARCHAR(100) NOT NULL COMMENT '规划名称',
  destination VARCHAR(100) NOT NULL COMMENT '目的地',
  location   VARCHAR(100) DEFAULT NULL COMMENT '所在省/国家',
  image      VARCHAR(255) DEFAULT NULL COMMENT '封面图（可空）',
  start_date DATE         DEFAULT NULL COMMENT '出发日期',
  days       JSON         DEFAULT NULL COMMENT '每日行程数组',
  waypoints  JSON         DEFAULT NULL COMMENT '途径点数组',
  is_public  TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否公开分享（1 公开 / 0 私有）',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅游规划';

INSERT INTO plan (id, user_id, name, destination, location, image, start_date, days, waypoints, is_public, created_at) VALUES
(1, 2, '敦煌三日游',  '敦煌莫高窟', '甘肃省', NULL, '2026-10-01',
 JSON_ARRAY(
   JSON_OBJECT('title','第一天','items',JSON_ARRAY(JSON_OBJECT('time','09:00','place','莫高窟','note','提前预约门票'))),
   JSON_OBJECT('title','第二天','items',JSON_ARRAY(JSON_OBJECT('time','08:00','place','鸣沙山月牙泉','note',''),JSON_OBJECT('time','16:00','place','沙洲夜市','note','尝尝驴肉黄面')))
 ),
 JSON_ARRAY(JSON_OBJECT('name','兰州','note','高铁中转')),
 1, '2026-08-10 10:00:00'),

(2, 2, '云南双人游',  '昆明-丽江-大理', '云南省', NULL, '2026-09-15',
 JSON_ARRAY(
   JSON_OBJECT('title','第一天','items',JSON_ARRAY(JSON_OBJECT('time','10:00','place','滇池','note','喂海鸥'))),
   JSON_OBJECT('title','第二天','items',JSON_ARRAY(JSON_OBJECT('time','09:00','place','洱海环湖','note','租电瓶车')))
 ),
 JSON_ARRAY(JSON_OBJECT('name','昆明','note','抵达后住一晚')),
 1, '2026-08-12 11:00:00'),

(3, 2, '迪士尼一日',  '上海迪士尼', '上海市', NULL, '2026-11-02',
 JSON_ARRAY(JSON_OBJECT('title','第一天','items',JSON_ARRAY(JSON_OBJECT('time','08:30','place','城堡','note','早点去排队')))),
 JSON_ARRAY(),
 0, '2026-08-14 09:30:00'),

(4, 3, '三亚度假',    '三亚亚龙湾', '海南省', NULL, '2026-09-01',
 JSON_ARRAY(JSON_OBJECT('title','第一天','items',JSON_ARRAY(JSON_OBJECT('time','15:00','place','亚龙湾海滩','note','')))),
 JSON_ARRAY(JSON_OBJECT('name','海口','note','机场中转')),
 1, '2026-08-16 16:00:00');

-- ------------------------------------------------------------
-- 4. note（社区笔记）
-- ------------------------------------------------------------
CREATE TABLE note (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
  author_id         BIGINT       NOT NULL COMMENT '作者用户ID',
  author_nickname   VARCHAR(50)  NOT NULL COMMENT '作者昵称（冗余）',
  title             VARCHAR(100) NOT NULL COMMENT '标题',
  content           TEXT         COMMENT '正文',
  image             TEXT         DEFAULT NULL COMMENT '封面图（URL 或 base64）',
  photos            JSON         DEFAULT NULL COMMENT '多图数组',
  location_name     VARCHAR(50)  DEFAULT NULL COMMENT '关联地点名',
  location_province VARCHAR(50)  DEFAULT NULL COMMENT '地点所在省/国家',
  likes             INT          NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数）',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (id),
  KEY idx_author (author_id),
  CONSTRAINT fk_note_user FOREIGN KEY (author_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区笔记';

-- likes 为演示冗余计数；note_like 表插入当前有效的点赞样例，二者不必逐条相等
INSERT INTO note (id, author_id, author_nickname, title, content, image, photos, location_name, location_province, likes, created_at) VALUES
(1, 2, '旅行者小A', '巴黎七日漫游指南',
 '沿着塞纳河漫步，路过旧书摊和咖啡香。卢浮宫建议提前网上订票，避开周二闭馆。傍晚的埃菲尔铁塔每隔整点亮灯五分钟，记得找好机位。',
 NULL, JSON_ARRAY(),
 '巴黎', '法国', 2400, '2026-08-20 10:00:00'),

(2, 2, '旅行者小A', '大理慢生活',
 '在大理，时间会变慢。洱海边的民宿推开窗就是苍山，早起看云，傍晚在古城吃菌子火锅。建议租电瓶车环湖，慢慢走走停停。',
 NULL, JSON_ARRAY(),
 '大理白族自治州', '云南省', 876, '2026-08-12 15:30:00'),

(3, 2, '旅行者小A', '京都雨夜散步',
 '雨夜的祇园安静又迷人，石板路映着纸灯笼的光。花见小路上偶遇艺伎，那一瞬间的京都比攻略里更美。',
 NULL, JSON_ARRAY(),
 '京都', '日本', 532, '2026-07-28 21:00:00'),

(4, 3, '张三', '武汉过早记',
 '碳水爱好者的天堂！热干面、豆皮、糊汤粉，早点摊的烟火气最抚凡人心。推荐粮道街和山海关路，本地人也常去。',
 NULL, JSON_ARRAY(),
 '武汉市', '湖北省', 210, '2026-08-22 08:30:00'),

(5, 4, '追光者', '拉萨的天空',
 '布达拉宫在晨光里泛着金光，转经筒的声音让人心安。八廓街的甜茶和藏面值得一试，高原反应别硬扛。',
 NULL, JSON_ARRAY(),
 '拉萨市', '西藏自治区', 980, '2026-08-19 13:00:00');

-- ------------------------------------------------------------
-- 5. note_like（点赞记录）
-- ------------------------------------------------------------
CREATE TABLE note_like (
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  note_id    BIGINT   NOT NULL COMMENT '笔记ID',
  user_id    BIGINT   NOT NULL COMMENT '点赞用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_note_user (note_id, user_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_like_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE,
  CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录';

INSERT INTO note_like (id, note_id, user_id, created_at) VALUES
(1, 1, 4, '2026-08-21 09:00:00'),
(2, 1, 5, '2026-08-21 10:30:00'),
(3, 3, 5, '2026-08-22 11:00:00'),
(4, 2, 3, '2026-08-23 12:00:00'),
(5, 5, 3, '2026-08-24 14:00:00');

-- ------------------------------------------------------------
-- 6. note_comment（笔记评论 / 回复）
--    只支持一级回复：parent_id 为 NULL 是顶层评论，非 NULL 表示对某条顶层评论的回复。
--    回复的 parent_id 一定指向顶层评论，不允许「回复的回复」。
--    删除顶层评论时，其下回复由 fk_comment_parent 的 ON DELETE CASCADE 一并删除。
-- ------------------------------------------------------------
CREATE TABLE note_comment (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  note_id    BIGINT       NOT NULL COMMENT '所属笔记ID',
  user_id    BIGINT       NOT NULL COMMENT '评论者用户ID',
  parent_id  BIGINT       DEFAULT NULL COMMENT '父评论ID（NULL=顶层评论；非空=回复，只允许一级）',
  content    VARCHAR(500) NOT NULL COMMENT '评论内容',
  likes      INT          NOT NULL DEFAULT 0 COMMENT '点赞数（冗余计数，与 comment_like 行数对应）',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (id),
  KEY idx_note (note_id),
  KEY idx_parent (parent_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_comment_note   FOREIGN KEY (note_id)   REFERENCES note (id)         ON DELETE CASCADE,
  CONSTRAINT fk_comment_user   FOREIGN KEY (user_id)   REFERENCES user (id)         ON DELETE CASCADE,
  CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES note_comment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记评论（parent_id 非空表示一级回复）';

-- 种子数据：顶层评论 id 1-6、回复 id 7-9（回复的 parent_id 都指向顶层评论）
INSERT INTO note_comment (id, note_id, user_id, parent_id, content, likes, created_at) VALUES
(1, 1, 3, NULL, '塞纳河那段的旧书摊真的有味道，我也在那待了一下午。', 12, '2026-08-20 11:20:00'),
(2, 1, 4, NULL, '卢浮宫周二人多吗？我打算周二去。', 5, '2026-08-20 14:05:00'),
(3, 2, 5, NULL, '洱海环湖租电瓶车多少钱一天？', 8, '2026-08-13 09:10:00'),
(4, 2, 3, NULL, '大理的菌子火锅是真的香，推荐古城南门那家。', 21, '2026-08-13 18:40:00'),
(5, 4, 4, NULL, '热干面配蛋酒才是本地吃法，下次试试。', 6, '2026-08-22 10:15:00'),
(6, 5, 2, NULL, '布达拉宫的票要提前多久预约呀？', 3, '2026-08-19 15:30:00'),
(7, 1, 2, 2,   '周二闭馆哦，换周三吧。', 4, '2026-08-20 15:00:00'),
(8, 2, 2, 3,   '旺季大概 80 一天，路边就有租的。', 2, '2026-08-13 10:05:00'),
(9, 1, 5, 1,   '同感，那边的咖啡也很便宜。', 1, '2026-08-21 08:30:00');

-- ------------------------------------------------------------
-- 7. comment_like（评论点赞记录）
--    评论与回复共用这一张表（回复也是 note_comment 的行）。
-- ------------------------------------------------------------
CREATE TABLE comment_like (
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  comment_id BIGINT   NOT NULL COMMENT '评论ID（含回复）',
  user_id    BIGINT   NOT NULL COMMENT '点赞用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_user (comment_id, user_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_clike_comment FOREIGN KEY (comment_id) REFERENCES note_comment (id) ON DELETE CASCADE,
  CONSTRAINT fk_clike_user    FOREIGN KEY (user_id)    REFERENCES user (id)         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞记录';

INSERT INTO comment_like (id, comment_id, user_id, created_at) VALUES
(1, 1, 2, '2026-08-20 12:00:00'),
(2, 1, 4, '2026-08-20 13:00:00'),
(3, 4, 2, '2026-08-13 19:00:00'),
(4, 7, 1, '2026-08-20 16:00:00'),
(5, 2, 5, '2026-08-20 15:30:00');

-- ------------------------------------------------------------
-- 8. feedback（建议反馈）
-- ------------------------------------------------------------
CREATE TABLE feedback (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  content    TEXT        NOT NULL COMMENT '反馈内容',
  contact    VARCHAR(100) DEFAULT NULL COMMENT '联系方式（可空）',
  images     JSON        DEFAULT NULL COMMENT '反馈截图URL数组（可空）',
  handled    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '处理状态：0 待处理 / 1 已处理',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建议反馈（匿名提交，不记录用户）';

INSERT INTO feedback (id, content, contact, handled, created_at) VALUES
(1, '【功能建议】希望增加夜间模式，晚上看地图太刺眼了', '', 1, '2026-08-25 20:00:00'),
(2, '【问题反馈】移动端打开地图有点卡', '', 0, '2026-08-26 10:00:00'),
(3, '【体验吐槽】点赞图标不太明显，建议换成实心的', 'wechat: xiaoA', 0, '2026-08-27 15:00:00');

-- ------------------------------------------------------------
-- 9. 热门目的地（不建表）
-- 社区侧栏「热门目的地 Top5」不再手工维护，
-- 由 visited_city 表按点亮用户数实时统计（见接口文档 4.6 / 5.1）。
-- ------------------------------------------------------------
