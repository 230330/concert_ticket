-- =============================================
-- 演唱会订票系统 - RBAC 权限体系初始化脚本
-- =============================================

-- 1. 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 2. 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `resource_type` VARCHAR(20) NOT NULL DEFAULT 'button' COMMENT '资源类型：menu-菜单，button-按钮',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID（0表示顶级）',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 3. 角色-权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- 4. 用户-角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- =============================================
-- 初始化角色数据
-- =============================================
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `status`) VALUES
(1, 'ADMIN', '超级管理员', '拥有系统全部权限', 1),
(2, 'CONCERT_MANAGER', '演唱会管理员', '管理演唱会、场次、场馆、艺人', 1),
(3, 'ORDER_MANAGER', '订单管理员', '管理订单、退款', 1),
(4, 'VIEWER', '数据查看员', '仅查看数据看板', 1);

-- =============================================
-- 初始化权限数据
-- =============================================
-- 演唱会管理
INSERT INTO `sys_permission` (`id`, `permission_code`, `permission_name`, `resource_type`, `parent_id`, `sort`) VALUES
(1,  'concert:list',   '演唱会列表',   'menu',   0, 1),
(2,  'concert:add',    '新增演唱会',   'button', 1, 2),
(3,  'concert:edit',   '编辑演唱会',   'button', 1, 3),
(4,  'concert:delete', '删除演唱会',   'button', 1, 4),
-- 场次管理
(5,  'show:list',      '场次列表',     'menu',   0, 5),
(6,  'show:add',       '新增场次',     'button', 5, 6),
(7,  'show:edit',      '编辑场次',     'button', 5, 7),
(8,  'show:delete',    '删除场次',     'button', 5, 8),
-- 场馆管理
(9,  'venue:list',     '场馆列表',     'menu',   0, 9),
(10, 'venue:add',      '新增场馆',     'button', 9, 10),
(11, 'venue:edit',     '编辑场馆',     'button', 9, 11),
(12, 'venue:delete',   '删除场馆',     'button', 9, 12),
-- 艺人管理
(13, 'artist:list',    '艺人列表',     'menu',   0, 13),
(14, 'artist:add',     '新增艺人',     'button', 13, 14),
(15, 'artist:edit',    '编辑艺人',     'button', 13, 15),
(16, 'artist:delete',  '删除艺人',     'button', 13, 16),
-- 票种与库存管理
(17, 'ticket:list',    '票种列表',     'menu',   0, 17),
(18, 'ticket:add',     '新增票种',     'button', 17, 18),
(19, 'ticket:edit',    '编辑票种',     'button', 17, 19),
(20, 'ticket:delete',  '删除票种',     'button', 17, 20),
(21, 'ticket:stock',   '库存调整',     'button', 17, 21),
-- 订单管理
(22, 'order:list',     '订单列表',     'menu',   0, 22),
(23, 'order:refund',   '订单退款',     'button', 22, 23),
(24, 'order:cancel',   '订单取消',     'button', 22, 24),
-- 用户管理
(25, 'user:list',      '用户列表',     'menu',   0, 25),
(26, 'user:ban',       '封禁用户',     'button', 25, 26),
(27, 'user:unban',     '解封用户',     'button', 25, 27),
-- 数据看板
(28, 'dashboard:view', '数据看板',     'menu',   0, 28);

-- =============================================
-- 初始化角色-权限关联
-- =============================================

-- 超级管理员（全部权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16),
(1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24),
(1, 25), (1, 26), (1, 27), (1, 28);

-- 演唱会管理员（演唱会+场次+场馆+艺人+票种权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8),
(2, 9), (2, 10), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16),
(2, 17), (2, 18), (2, 19), (2, 20), (2, 21);

-- 订单管理员（订单+用户查看权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3, 22), (3, 23), (3, 24), (3, 25);

-- 数据查看员（仅数据看板）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4, 28);

-- =============================================
-- 创建默认管理员账号（密码：admin123）
-- 如需创建管理员，请先注册用户，再执行以下SQL绑定角色
-- 假设管理员用户ID为1（请根据实际情况修改）
-- =============================================
-- INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);
