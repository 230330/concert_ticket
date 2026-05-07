-- 为 user 表添加昵称最后修改时间字段
ALTER TABLE `user` ADD COLUMN `nickname_last_modified` datetime DEFAULT NULL COMMENT '昵称最后修改时间' AFTER `avatar`;
-- 2026-5-7 添加场次状态字段
ALTER TABLE concert_ticket.`show` MODIFY COLUMN status tinyint DEFAULT 1 NOT NULL COMMENT '状态：0未开售 1售票中 2已售罄 3已结束 4已取消';
