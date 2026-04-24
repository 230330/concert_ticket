ALTER TABLE `order`
CHANGE COLUMN `pickup_code` varchar(20) DEFAULT NULL COMMENT '取票码';

-- 同时重命名索引
ALTER TABLE `order` DROP INDEX `idx_ticket_code`;
ALTER TABLE `order` ADD INDEX `idx_pickup_code` (`pickup_code`);
