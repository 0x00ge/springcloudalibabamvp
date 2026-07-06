-- 公共库：除 user 外的业务表都放在这里。
-- 当前包含：商品表、订单表、订单消息幂等表。
CREATE DATABASE IF NOT EXISTS `mvp_common` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mvp_common`;

CREATE TABLE IF NOT EXISTS `t_goods` (
    `id`              CHAR(32)      NOT NULL COMMENT '商品ID，32位无横杠UUIDv7格式',
    `name`            VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `seckill_price`   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    `total_stock`     INT           NOT NULL COMMENT '总库存（仅作Redis初始化种子）',
    `limit_per_user`  INT           NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `start_time`      DATETIME      NOT NULL COMMENT '秒杀开始时间',
    `end_time`        DATETIME      NOT NULL COMMENT '秒杀结束时间',
    `status`          TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_goods_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `t_order` (
    `id`               CHAR(32)      NOT NULL COMMENT '订单ID，32位无横杠UUIDv7格式',
    `goods_id`         CHAR(32)      NOT NULL COMMENT '商品ID',
    `user_id`          CHAR(32)      NOT NULL COMMENT '用户ID',
    `buy_count`        INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    `amount`           DECIMAL(10,2) NOT NULL COMMENT '订单金额 = 秒杀价 × 购买数量',
    `status`           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已取消',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_user_goods` (`user_id`, `goods_id`) COMMENT '防重索引：同一用户对同一商品只能下单一次',
    KEY `idx_order_user_id` (`user_id`) COMMENT '按用户查询订单',
    KEY `idx_order_goods_id` (`goods_id`) COMMENT '按商品查询订单',
    KEY `idx_order_created_at` (`created_at`) COMMENT '按创建时间查询订单'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `t_order_message_processed` (
    `id`           CHAR(32)        NOT NULL COMMENT '记录ID，32位无横杠UUIDv7格式',
    `message_id`   VARCHAR(64)     NOT NULL COMMENT 'RocketMQ 消息唯一 ID',
    `business_key` VARCHAR(100)    NOT NULL COMMENT '业务唯一键，格式：{userId}#{goodsId}',
    `processed_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息处理时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`) COMMENT '消息ID唯一索引，防止重复处理',
    KEY `idx_business_key` (`business_key`) COMMENT '业务键索引',
    KEY `idx_processed_at` (`processed_at`) COMMENT '处理时间索引，用于清理历史数据'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单事件消息已处理记录表';
