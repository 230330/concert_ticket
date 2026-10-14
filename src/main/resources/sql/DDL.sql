-- 说明：本文件记录建库之后的增量结构变更，只在已存在的库上执行。
-- 全新环境请用同目录的 backup.sql 初始化完整表结构与初始数据（含 RBAC 角色权限）。

-- 为 user 表添加昵称最后修改时间字段
ALTER TABLE `user` ADD COLUMN `nickname_last_modified` datetime DEFAULT NULL COMMENT '昵称最后修改时间' AFTER `avatar`;
-- 2026-5-7 添加场次状态字段
ALTER TABLE concert_ticket.`show` MODIFY COLUMN status tinyint DEFAULT 1 NOT NULL COMMENT '状态：0未开售 1售票中 2已售罄 3已结束 4已取消';
