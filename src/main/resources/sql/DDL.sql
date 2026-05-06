-- 为 user 表添加昵称最后修改时间字段
ALTER TABLE `user` ADD COLUMN `nickname_last_modified` datetime DEFAULT NULL COMMENT '昵称最后修改时间' AFTER `avatar`;
