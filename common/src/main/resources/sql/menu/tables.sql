CREATE TABLE IF NOT EXISTS `t_menu` (
    `id`          CHAR(32)      NOT NULL COMMENT '菜单ID，32位无横杠UUIDv7格式',
    `parent_id`   CHAR(32)      NULL DEFAULT NULL COMMENT '父菜单ID，NULL表示一级菜单',
    `title`       VARCHAR(50)   NOT NULL COMMENT '菜单显示名称',
    `path`        VARCHAR(200)  NOT NULL COMMENT '前端路由路径',
    `icon`        VARCHAR(50)   NULL DEFAULT NULL COMMENT '菜单图标名称，对应前端 Element Plus 图标',
    `level`       INT           NOT NULL DEFAULT 1 COMMENT '菜单层级，从1开始，支持多级菜单',
    `sort_order`  INT           NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `user_id`     CHAR(32)      NOT NULL COMMENT '用户ID',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME      NULL DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_menu_user_parent_sort` (`user_id`, `parent_id`, `sort_order`),
    KEY `idx_menu_deleted` (`deleted_at`),
    KEY `idx_menu_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';