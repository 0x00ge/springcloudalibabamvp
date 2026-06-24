-- ============================================
-- service-user-0 数据库表结构
-- ============================================
-- 用户服务负责用户注册、登录、鉴权等功能
-- 依赖表：t_user
-- ============================================

CREATE TABLE IF NOT EXISTS `t_user` (
    -- 核心标识
    `id`            CHAR(32)        NOT NULL COMMENT '用户ID，32位无横杠UUIDv7格式',

    -- 登录凭证
    `phone`         VARCHAR(20)     NOT NULL COMMENT '手机号',
    `email`         VARCHAR(100)    NULL DEFAULT NULL COMMENT '邮箱',
    `password_hash` VARCHAR(100)    NOT NULL COMMENT 'BCrypt加密后的密码',

    -- 基本信息
    `name`          VARCHAR(50)     NOT NULL COMMENT '用户名称',
    `avatar_url`    VARCHAR(500)    NULL DEFAULT NULL COMMENT '头像URL',
    `gender`        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `birthday`      DATE            NULL DEFAULT NULL COMMENT '出生日期',

    -- 状态控制
    `permission`    VARCHAR(20)     NOT NULL DEFAULT 'USER' COMMENT '用户权限: ADMIN-管理员, USER-普通用户',
    `status`        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常, 2-注销',
    `last_login_at` DATETIME        NULL DEFAULT NULL COMMENT '最后登录时间',

    -- 时间戳
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    DATETIME        NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_permission` (`permission`),
    KEY `idx_user_deleted` (`deleted_at`),
    KEY `idx_user_created_at` (`created_at`),
    KEY `idx_user_last_login_at` (`last_login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户菜单表
CREATE TABLE IF NOT EXISTS `t_user_menu` (
    -- 核心标识
    `id`          CHAR(32)      NOT NULL COMMENT '菜单ID，32位无横杠UUIDv7格式',

    -- 关联用户
    `user_id`     CHAR(32)      NOT NULL COMMENT '用户ID',

    -- 树形结构
    `parent_id`   CHAR(32)      NULL DEFAULT NULL COMMENT '父菜单ID，NULL表示一级菜单',
    `level`       INT           NOT NULL DEFAULT 1 COMMENT '菜单层级: 1-一级菜单, 2-二级菜单, 依次递增',
    `sort_order`  INT           NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',

    -- 菜单展示
    `title`       VARCHAR(50)   NOT NULL COMMENT '菜单显示名称',
    `path`        VARCHAR(200)  NOT NULL COMMENT '前端路由路径',
    `icon`        VARCHAR(50)   NULL DEFAULT NULL COMMENT '菜单图标名称，对应前端 Element Plus 图标',

    -- 时间戳
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME      NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_menu_path` (`user_id`, `path`),
    KEY `idx_user_menu_user_parent_sort` (`user_id`, `parent_id`, `sort_order`),
    KEY `idx_user_menu_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户菜单表';
