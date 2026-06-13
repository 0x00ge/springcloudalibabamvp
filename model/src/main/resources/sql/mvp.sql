CREATE TABLE `t_user` (
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
    `status`        TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常, 2-注销',
    `last_login_at` DATETIME        NULL DEFAULT NULL COMMENT '最后登录时间',

    -- 时间戳
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    DATETIME        NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_deleted` (`deleted_at`),
    KEY `idx_user_created_at` (`created_at`),
    KEY `idx_user_last_login_at` (`last_login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


CREATE TABLE `t_seckill_goods` (
    -- 核心标识
    `id`              CHAR(32)      NOT NULL COMMENT '秒杀商品ID，32位无横杠UUIDv7格式',

    -- 商品信息
    `goods_name`      VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `seckill_price`   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',

    -- 库存控制
    `total_stock`     INT           NOT NULL COMMENT '总库存',
    `limit_per_user`  INT           NOT NULL DEFAULT 1 COMMENT '每人限购数量',

    -- 活动时间窗口
    `start_time`      DATETIME      NOT NULL COMMENT '秒杀开始时间',
    `end_time`        DATETIME      NOT NULL COMMENT '秒杀结束时间',

    -- 状态
    `status`          TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',

    -- 时间戳
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_seckill_goods_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀商品表';


CREATE TABLE `t_seckill_order` (
    -- 核心标识
    `id`               CHAR(32)      NOT NULL COMMENT '秒杀订单ID，32位无横杠UUIDv7格式',

    -- 关联信息
    `seckill_goods_id` CHAR(32)      NOT NULL COMMENT '秒杀商品ID',
    `user_id`          CHAR(32)      NOT NULL COMMENT '用户ID',

    -- 订单信息
    `buy_count`        INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    `order_amount`     DECIMAL(10,2) NOT NULL COMMENT '订单金额 = 秒杀价 × 购买数量',
    `status`           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已取消',

    -- 时间戳
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seckill_order_user_goods` (`user_id`, `seckill_goods_id`),
    KEY `idx_seckill_order_goods_id` (`seckill_goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';
