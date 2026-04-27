-- 清空现有用户表（慎用，会删除所有已有数据）
-- TRUNCATE TABLE `user`;

-- 插入测试用户
-- 密码加密规则：BCrypt 加密 "123456"，以下为常用的固定密文（强度10）
-- 注意：如果需要不同的密码，请自行使用 BCryptPasswordEncoder 生成
INSERT INTO `user` (`phone`, `password`, `nickname`, `avatar`, `status`) VALUES
('13812345678', '$2a$10$Nk2cLPLnF8ok5JXjH5lFkeA5q7kQZQzQ7Wq6qYp/QmY6jQ5iL6LmK', '测试用户1', NULL, 1),
('13912345678', '$2a$10$Nk2cLPLnF8ok5JXjH5lFkeA5q7kQZQzQ7Wq6qYp/QmY6jQ5iL6LmK', '周杰伦粉丝', NULL, 1),
('15012345678', '$2a$10$Nk2cLPLnF8ok5JXjH5lFkeA5q7kQZQzQ7Wq6qYp/QmY6jQ5iL6LmK', '林俊杰歌迷', NULL, 1);