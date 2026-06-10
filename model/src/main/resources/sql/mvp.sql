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


CREATE TABLE `t_seckill_activity` (
    -- 核心标识
    `id`            CHAR(32)      NOT NULL COMMENT '活动ID，32位无横杠UUIDv7格式',

    -- 活动信息
    `activity_name` VARCHAR(100)  NOT NULL COMMENT '活动名称',
    `activity_desc` VARCHAR(255)  NULL DEFAULT NULL COMMENT '活动描述',
    `status`        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态: 0-未开始, 1-进行中, 2-已结束, 3-已下线',
    `start_time`    DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`      DATETIME      NOT NULL COMMENT '结束时间',

    -- 时间戳
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    DATETIME      NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    KEY `idx_activity_status` (`status`),
    KEY `idx_activity_start_time` (`start_time`),
    KEY `idx_activity_end_time` (`end_time`),
    KEY `idx_activity_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';


CREATE TABLE `t_seckill_goods` (
    -- 核心标识
    `id`              CHAR(32)      NOT NULL COMMENT '秒杀商品ID，32位无横杠UUIDv7格式',

    -- 关联信息
    `activity_id`     CHAR(32)      NOT NULL COMMENT '活动ID',
    `goods_id`        CHAR(32)      NOT NULL COMMENT '商品ID',

    -- 商品信息
    `goods_name`      VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `original_price`  DECIMAL(10,2) NOT NULL COMMENT '原价',
    `seckill_price`   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',

    -- 库存控制
    `total_stock`     INT           NOT NULL COMMENT '总库存',
    `available_stock` INT           NOT NULL COMMENT '可用库存',
    `frozen_stock`    INT           NOT NULL DEFAULT 0 COMMENT '冻结库存',
    `limit_per_user`  INT           NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `status`          TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `version`         INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',

    -- 时间戳
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      DATETIME      NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seckill_goods_activity_goods` (`activity_id`, `goods_id`),
    KEY `idx_seckill_goods_activity_id` (`activity_id`),
    KEY `idx_seckill_goods_goods_id` (`goods_id`),
    KEY `idx_seckill_goods_status` (`status`),
    KEY `idx_seckill_goods_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀商品表';


CREATE TABLE `t_seckill_order` (
    -- 核心标识
    `id`               CHAR(32)      NOT NULL COMMENT '秒杀订单ID，32位无横杠UUIDv7格式',

    -- 业务标识
    `order_no`         VARCHAR(64)   NOT NULL COMMENT '订单号',

    -- 关联信息
    `activity_id`      CHAR(32)      NOT NULL COMMENT '活动ID',
    `seckill_goods_id` CHAR(32)      NOT NULL COMMENT '秒杀商品ID',
    `goods_id`         CHAR(32)      NOT NULL COMMENT '商品ID',
    `user_id`          CHAR(32)      NOT NULL COMMENT '用户ID',

    -- 订单信息
    `buy_count`        INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    `order_amount`     DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status`           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已取消, 3-已完成',
    `pay_time`         DATETIME      NULL DEFAULT NULL COMMENT '支付时间',
    `cancel_time`      DATETIME      NULL DEFAULT NULL COMMENT '取消时间',

    -- 时间戳
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       DATETIME      NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seckill_order_no` (`order_no`),
    UNIQUE KEY `uk_seckill_order_user_activity_goods` (`user_id`, `activity_id`, `goods_id`),
    KEY `idx_seckill_order_user_id` (`user_id`),
    KEY `idx_seckill_order_activity_id` (`activity_id`),
    KEY `idx_seckill_order_goods_id` (`goods_id`),
    KEY `idx_seckill_order_status` (`status`),
    KEY `idx_seckill_order_created_at` (`created_at`),
    KEY `idx_seckill_order_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';


CREATE TABLE `t_seckill_order_log` (
    -- 核心标识
    `id`          CHAR(32)     NOT NULL COMMENT '日志ID，32位无横杠UUIDv7格式',

    -- 业务标识
    `request_no`  VARCHAR(64)  NOT NULL COMMENT '请求流水号',

    -- 关联信息
    `user_id`     CHAR(32)     NOT NULL COMMENT '用户ID',
    `activity_id` CHAR(32)     NOT NULL COMMENT '活动ID',
    `goods_id`    CHAR(32)     NOT NULL COMMENT '商品ID',

    -- 处理状态
    `status`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '状态: 0-处理中, 1-成功, 2-失败',
    `remark`      VARCHAR(255) NULL DEFAULT NULL COMMENT '处理备注',

    -- 时间戳
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  DATETIME     NULL DEFAULT NULL COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_seckill_order_log_request_no` (`request_no`),
    KEY `idx_seckill_order_log_user_activity_goods` (`user_id`, `activity_id`, `goods_id`),
    KEY `idx_seckill_order_log_status` (`status`),
    KEY `idx_seckill_order_log_deleted` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀请求日志表';
