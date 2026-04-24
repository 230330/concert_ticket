-- ======================================================
-- 演唱会订票系统测试数据
-- 包含：艺人、演出、演出艺人关联、场馆、座位区域、座位、场次、票品
-- 基于 2025-2026 真实演唱会信息制作
-- ======================================================

-- 1. 插入艺人数据 (artist)
INSERT INTO `artist` (`id`, `name`, `avatar`, `intro`) VALUES
(1, '周杰伦', 'https://example.com/artist/jaychou.jpg', '华语流行乐男歌手、音乐人，被誉为“亚洲流行天王”。'),
(2, '林俊杰', 'https://example.com/artist/linjunjie.jpg', '华语流行乐男歌手、词曲创作人、音乐制作人。'),
(3, '邓紫棋', 'https://example.com/artist/dengziqi.jpg', '华语创作型女歌手，以高亢嗓音和创作才华著称。'),
(4, '薛之谦', 'https://example.com/artist/xuezhiqian.jpg', '华语流行乐男歌手、音乐制作人、影视演员。'),
(5, '五月天', 'https://example.com/artist/mayday.jpg', '中国台湾摇滚乐团，华语乐坛最具影响力的乐团之一。'),
(6, '张杰', 'https://example.com/artist/zhangjie.jpg', '华语流行男歌手，以高亢嘹亮的嗓音著称。'),
(7, '周深', 'https://example.com/artist/zhoushen.jpg', '中国内地男歌手，以其空灵独特的嗓音闻名。'),
(8, '孙燕姿', 'https://example.com/artist/sunyanzi.jpg', '新加坡籍华语流行乐女歌手，以独特唱腔和真挚情感著称。'),
(9, '王力宏', 'https://example.com/artist/wanglihong.jpg', '华语流行乐男歌手、音乐制作人、演员、导演。'),
(10, '伍佰', 'https://example.com/artist/wubai.jpg', '中国台湾摇滚乐男歌手、音乐人、演员。');


-- 2. 插入演出数据 (concert)
INSERT INTO `concert` (`id`, `name`, `poster`, `description`, `status`) VALUES
(1, '周杰伦「嘉年华」世界巡回演唱会', 'https://example.com/poster/jaychou_carnival.jpg', '周杰伦「嘉年华」世界巡回演唱会，以欢乐、嘉年华为主题，带来一场视听盛宴。', 2),
(2, '林俊杰「JJ20 FINAL LAP」世界巡回演唱会', 'https://example.com/poster/linjunjie_jj20.jpg', '林俊杰 JJ20 世界巡回演唱会最终章，回顾 20 年音乐历程，感动呈现。', 2),
(3, '邓紫棋「GLORIA」世界巡回演唱会', 'https://example.com/poster/dengziqi_gloria.jpg', '邓紫棋「GLORIA」世界巡回演唱会，以爱与信仰为主题，展现全新音乐篇章。', 2),
(4, '薛之谦「天外来物」巡回演唱会', 'https://example.com/poster/xuezhiqian_extraterrestrial.jpg', '薛之谦「天外来物」巡回演唱会，以科幻、未来感为概念，打造沉浸式音乐体验。', 2),
(5, '五月天「回到那一天」25周年巡回演唱会', 'https://example.com/poster/mayday_backtothatday.jpg', '五月天成军 25 周年纪念巡回演唱会，重温经典歌曲，与歌迷共同回忆青春。', 2),
(6, '张杰「未·LIVE—开往1982」巡回演唱会', 'https://example.com/poster/zhangjie_openlive.jpg', '张杰「未·LIVE—开往1982」巡回演唱会，以音乐列车为概念，带领歌迷开启音乐之旅。', 2),
(7, '周深「深深的」巡回演唱会', 'https://example.com/poster/zhoushen_deep.jpg', '周深「深深的」巡回演唱会，以其空灵嗓音和深情演绎，为歌迷带来一场治愈之旅。', 2),
(8, '孙燕姿「就在日落以后」巡回演唱会', 'https://example.com/poster/sunyanzi_afterthesunset.jpg', '孙燕姿「就在日落以后」巡回演唱会，以温暖、治愈为主题，唱出岁月沉淀后的感动。', 2),
(9, '王力宏「最好的地方II」巡回演唱会', 'https://example.com/poster/wanglihong_bestplace2.jpg', '王力宏「最好的地方II」巡回演唱会，出道 30 周年里程碑巡演，舞美全新升级。', 2),
(10, '伍佰 & China Blue「ROCK STAR 2」巡回演唱会', 'https://example.com/poster/wubai_rockstar2.jpg', '伍佰 & China Blue「ROCK STAR 2」巡回演唱会，摇滚盛宴，万人合唱。', 2);


-- 3. 插入演出艺人关联数据 (concert_artist)
INSERT INTO `concert_artist` (`concert_id`, `artist_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 7),
(8, 8),
(9, 9),
(10, 10);


-- 4. 插入场馆数据 (venue)
INSERT INTO `venue` (`id`, `name`, `address`, `city`, `latitude`, `longitude`) VALUES
(1, '杭州奥体中心体育场（大莲花）', '杭州市滨江区飞虹路 3 号', '杭州', 30.22500000, 120.22500000),
(2, '上海体育场', '上海市徐汇区天钥桥路 666 号', '上海', 31.18300000, 121.43900000),
(3, '广东奥林匹克体育中心', '广州市天河区大观南路 36 号', '广州', 23.13700000, 113.40200000),
(4, '深圳大运中心体育场', '深圳市龙岗区龙翔大道 3001 号', '深圳', 22.69700000, 114.22600000),
(5, '成都东安湖体育公园主体育场', '成都市龙泉驿区东安湖路', '成都', 30.58000000, 104.25700000),
(6, '武汉体育中心主体育场', '武汉市蔡甸区车城北路 58 号', '武汉', 30.50900000, 114.16900000),
(7, '西安奥体中心体育馆', '西安市灞桥区西滨路与向东路交叉口东北角', '西安', 34.39400000, 109.02100000),
(8, '南京奥体中心体育场', '南京市建邺区江东中路 222 号', '南京', 32.00500000, 118.72100000),
(9, '国家体育场（鸟巢）', '北京市朝阳区国家体育场南路 1 号', '北京', 39.99200000, 116.39700000),
(10, '重庆奥体中心体育场', '重庆市九龙坡区袁家岗奥体路', '重庆', 29.53700000, 106.51400000),
(11, '苏州奥林匹克体育中心体育场', '苏州市苏州工业园区中新大道东 999 号', '苏州', 31.31500000, 120.71500000),
(12, '长沙贺龙体育中心体育场', '长沙市天心区劳动西路 358 号', '长沙', 28.18400000, 112.97300000);


-- 5. 插入座位区域数据 (seat_area)
INSERT INTO `seat_area` (`id`, `venue_id`, `name`, `color`, `rows`, `cols`) VALUES
(1, 1, '内场 VIP 区', '#FF6B6B', 10, 20),
(2, 1, '内场 A 区', '#4ECDC4', 15, 25),
(3, 1, '看台 B 区', '#FFE66D', 20, 30),
(4, 1, '看台 C 区', '#95E77E', 20, 30),
(5, 2, '内场 VIP 区', '#FF6B6B', 8, 20),
(6, 2, '内场 A 区', '#4ECDC4', 12, 25),
(7, 2, '看台 B 区', '#FFE66D', 18, 30),
(8, 2, '看台 C 区', '#95E77E', 18, 30),
(9, 3, '内场 VIP 区', '#FF6B6B', 10, 20),
(10, 3, '内场 A 区', '#4ECDC4', 15, 25),
(11, 3, '看台 B 区', '#FFE66D', 20, 30),
(12, 4, '内场 VIP 区', '#FF6B6B', 8, 20),
(13, 4, '内场 A 区', '#4ECDC4', 12, 25),
(14, 4, '看台 B 区', '#FFE66D', 18, 30),
(15, 5, '内场 VIP 区', '#FF6B6B', 10, 20),
(16, 5, '内场 A 区', '#4ECDC4', 15, 25),
(17, 5, '看台 B 区', '#FFE66D', 20, 30),
(18, 6, '内场 VIP 区', '#FF6B6B', 10, 20),
(19, 6, '内场 A 区', '#4ECDC4', 15, 25),
(20, 6, '看台 B 区', '#FFE66D', 20, 30),
(21, 7, '内场 VIP 区', '#FF6B6B', 8, 20),
(22, 7, '内场 A 区', '#4ECDC4', 12, 25),
(23, 7, '看台 B 区', '#FFE66D', 18, 30),
(24, 8, '内场 VIP 区', '#FF6B6B', 10, 20),
(25, 8, '内场 A 区', '#4ECDC4', 15, 25),
(26, 8, '看台 B 区', '#FFE66D', 20, 30),
(27, 9, '内场 VIP 区', '#FF6B6B', 12, 25),
(28, 9, '内场 A 区', '#4ECDC4', 18, 30),
(29, 9, '看台 B 区', '#FFE66D', 25, 35),
(30, 9, '看台 C 区', '#95E77E', 25, 35),
(31, 10, '内场 VIP 区', '#FF6B6B', 10, 20),
(32, 10, '内场 A 区', '#4ECDC4', 15, 25),
(33, 10, '看台 B 区', '#FFE66D', 20, 30),
(34, 11, '内场 VIP 区', '#FF6B6B', 10, 20),
(35, 11, '内场 A 区', '#4ECDC4', 15, 25),
(36, 11, '看台 B 区', '#FFE66D', 20, 30),
(37, 12, '内场 VIP 区', '#FF6B6B', 8, 20),
(38, 12, '内场 A 区', '#4ECDC4', 12, 25),
(39, 12, '看台 B 区', '#FFE66D', 18, 30);


-- 6. 插入座位数据 (seat) - 为每个区域生成座位
-- 杭州奥体中心体育场 - 内场 VIP 区 (rows=10, cols=20)
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 1, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 杭州奥体中心体育场 - 内场 A 区 (rows=15, cols=25)
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 2, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 杭州奥体中心体育场 - 看台 B 区 (rows=20, cols=30)
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 3, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 杭州奥体中心体育场 - 看台 C 区 (rows=20, cols=30)
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 4, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 为其他场馆生成座位数据（简化版，只生成关键区域）
-- 上海体育场 - 内场 VIP 区
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 5, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 广东奥林匹克体育中心 - 内场 VIP 区
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 9, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 深圳大运中心体育场 - 内场 VIP 区
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 12, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;
-- 国家体育场（鸟巢）- 内场 VIP 区
INSERT INTO `seat` (`area_id`, `row_code`, `col_num`) SELECT 27, CHAR(64 + rn) AS row_code, cn FROM (SELECT @rn := @rn + 1 AS rn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) t, (SELECT @rn := 0) r) rows, (SELECT @cn := @cn + 1 AS cn FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) t, (SELECT @cn := 0) c) cols ORDER BY rn, cn;


-- 7. 插入场次数据 (show)
-- 周杰伦杭州站（已售罄状态，开售时间已过）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(1, 1, '2026-04-03', '19:30:00', '2026-03-01 10:00:00', '2026-04-02 23:59:59'),
(1, 1, '2026-04-04', '19:30:00', '2026-03-01 10:00:00', '2026-04-03 23:59:59'),
(1, 1, '2026-04-05', '19:30:00', '2026-03-01 10:00:00', '2026-04-04 23:59:59');
-- 林俊杰上海站（热卖中，开售时间已过）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(2, 2, '2026-03-28', '19:30:00', '2026-02-15 10:00:00', '2026-03-27 23:59:59'),
(2, 2, '2026-03-29', '19:30:00', '2026-02-15 10:00:00', '2026-03-28 23:59:59'),
(2, 2, '2026-03-30', '19:30:00', '2026-02-15 10:00:00', '2026-03-29 23:59:59');
-- 邓紫棋深圳站（热卖中）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(3, 4, '2026-03-14', '19:30:00', '2026-02-01 10:00:00', '2026-03-13 23:59:59'),
(3, 4, '2026-03-15', '19:30:00', '2026-02-01 10:00:00', '2026-03-14 23:59:59'),
(3, 4, '2026-03-16', '19:30:00', '2026-02-01 10:00:00', '2026-03-15 23:59:59');
-- 薛之谦长沙站（即将开售，开售时间在未来）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(4, 12, '2026-05-30', '19:30:00', '2026-04-25 10:00:00', '2026-05-29 23:59:59'),
(4, 12, '2026-05-31', '19:30:00', '2026-04-25 10:00:00', '2026-05-30 23:59:59'),
(4, 12, '2026-06-01', '19:30:00', '2026-04-25 10:00:00', '2026-05-31 23:59:59');
-- 五月天成都站（即将开售）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(5, 5, '2026-05-09', '19:30:00', '2026-04-10 10:00:00', '2026-05-08 23:59:59'),
(5, 5, '2026-05-10', '19:30:00', '2026-04-10 10:00:00', '2026-05-09 23:59:59'),
(5, 5, '2026-05-11', '19:30:00', '2026-04-10 10:00:00', '2026-05-10 23:59:59');
-- 张杰西安站（热卖中）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(6, 7, '2026-05-02', '19:30:00', '2026-03-15 10:00:00', '2026-05-01 23:59:59'),
(6, 7, '2026-05-03', '19:30:00', '2026-03-15 10:00:00', '2026-05-02 23:59:59'),
(6, 7, '2026-05-04', '19:30:00', '2026-03-15 10:00:00', '2026-05-03 23:59:59');
-- 周深广州站（热卖中）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(7, 3, '2026-04-18', '19:30:00', '2026-03-01 10:00:00', '2026-04-17 23:59:59'),
(7, 3, '2026-04-19', '19:30:00', '2026-03-01 10:00:00', '2026-04-18 23:59:59');
-- 孙燕姿苏州站（即将开售）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(8, 11, '2026-04-10', '19:30:00', '2026-03-20 10:00:00', '2026-04-09 23:59:59'),
(8, 11, '2026-04-12', '19:30:00', '2026-03-20 10:00:00', '2026-04-11 23:59:59');
-- 王力宏杭州站（即将开售）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(9, 1, '2026-05-30', '19:30:00', '2026-04-20 10:00:00', '2026-05-29 23:59:59'),
(9, 1, '2026-05-31', '19:30:00', '2026-04-20 10:00:00', '2026-05-30 23:59:59');
-- 伍佰西安站（即将开售）
INSERT INTO `show` (`concert_id`, `venue_id`, `show_date`, `show_time`, `sale_start_time`, `sale_end_time`) VALUES
(10, 7, '2026-07-18', '19:00:00', '2026-05-01 10:00:00', '2026-07-17 23:59:59'),
(10, 7, '2026-07-19', '19:00:00', '2026-05-01 10:00:00', '2026-07-18 23:59:59');


-- 8. 插入票品数据 (ticket_type)
-- 周杰伦杭州站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(1, 1, 1880.00, 200, 198),
(1, 2, 1280.00, 375, 370),
(1, 3, 880.00, 600, 590),
(1, 4, 580.00, 600, 585),
(2, 1, 1880.00, 200, 0),
(2, 2, 1280.00, 375, 0),
(2, 3, 880.00, 600, 0),
(2, 4, 580.00, 600, 0),
(3, 1, 1880.00, 200, 0),
(3, 2, 1280.00, 375, 0),
(3, 3, 880.00, 600, 0),
(3, 4, 580.00, 600, 0);
-- 林俊杰上海站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(4, 5, 1680.00, 160, 80),
(4, 6, 1180.00, 300, 150),
(4, 7, 780.00, 540, 270),
(4, 8, 480.00, 540, 260),
(5, 5, 1680.00, 160, 0),
(5, 6, 1180.00, 300, 0),
(5, 7, 780.00, 540, 0),
(5, 8, 480.00, 540, 0),
(6, 5, 1680.00, 160, 0),
(6, 6, 1180.00, 300, 0),
(6, 7, 780.00, 540, 0),
(6, 8, 480.00, 540, 0);
-- 邓紫棋深圳站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(7, 12, 1580.00, 160, 60),
(7, 13, 1080.00, 300, 120),
(7, 14, 680.00, 540, 200),
(8, 12, 1580.00, 160, 0),
(8, 13, 1080.00, 300, 0),
(8, 14, 680.00, 540, 0),
(9, 12, 1580.00, 160, 0),
(9, 13, 1080.00, 300, 0),
(9, 14, 680.00, 540, 0);
-- 薛之谦长沙站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(10, 37, 1380.00, 160, 0),
(10, 38, 880.00, 300, 0),
(10, 39, 480.00, 540, 0),
(11, 37, 1380.00, 160, 0),
(11, 38, 880.00, 300, 0),
(11, 39, 480.00, 540, 0),
(12, 37, 1380.00, 160, 0),
(12, 38, 880.00, 300, 0),
(12, 39, 480.00, 540, 0);
-- 五月天成都站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(13, 15, 1550.00, 200, 0),
(13, 16, 1050.00, 375, 0),
(13, 17, 750.00, 600, 0),
(14, 15, 1550.00, 200, 0),
(14, 16, 1050.00, 375, 0),
(14, 17, 750.00, 600, 0),
(15, 15, 1550.00, 200, 0),
(15, 16, 1050.00, 375, 0),
(15, 17, 750.00, 600, 0);
-- 张杰西安站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(16, 21, 1480.00, 160, 50),
(16, 22, 980.00, 300, 100),
(16, 23, 580.00, 540, 150),
(17, 21, 1480.00, 160, 0),
(17, 22, 980.00, 300, 0),
(17, 23, 580.00, 540, 0),
(18, 21, 1480.00, 160, 0),
(18, 22, 980.00, 300, 0),
(18, 23, 580.00, 540, 0);
-- 周深广州站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(19, 9, 1280.00, 200, 80),
(19, 10, 880.00, 375, 150),
(19, 11, 580.00, 600, 200),
(20, 9, 1280.00, 200, 0),
(20, 10, 880.00, 375, 0),
(20, 11, 580.00, 600, 0);
-- 孙燕姿苏州站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(21, 34, 1380.00, 200, 0),
(21, 35, 880.00, 375, 0),
(21, 36, 580.00, 600, 0),
(22, 34, 1380.00, 200, 0),
(22, 35, 880.00, 375, 0),
(22, 36, 580.00, 600, 0);
-- 王力宏杭州站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(23, 1, 1880.00, 200, 0),
(23, 2, 1280.00, 375, 0),
(23, 3, 880.00, 600, 0),
(23, 4, 580.00, 600, 0),
(24, 1, 1880.00, 200, 0),
(24, 2, 1280.00, 375, 0),
(24, 3, 880.00, 600, 0),
(24, 4, 580.00, 600, 0);
-- 伍佰西安站票品
INSERT INTO `ticket_type` (`show_id`, `area_id`, `price`, `total_quantity`, `sold_quantity`) VALUES
(25, 21, 1280.00, 160, 0),
(25, 22, 880.00, 300, 0),
(25, 23, 480.00, 540, 0),
(26, 21, 1280.00, 160, 0),
(26, 22, 880.00, 300, 0),
(26, 23, 480.00, 540, 0);