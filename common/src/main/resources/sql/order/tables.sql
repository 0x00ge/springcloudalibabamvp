-- ============================================
-- service-order-0 数据库表结构
-- ============================================
-- 订单服务负责秒杀下单、订单管理、RocketMQ 异步处理
-- 依赖表：t_order, t_order_message_processed
-- ============================================

-- 订单表
CREATE TABLE IF NOT EXISTS `t_order` (
    -- 核心标识
    `id`               CHAR(32)      NOT NULL COMMENT '订单ID，32位无横杠UUIDv7格式',

    -- 关联信息
    `goods_id`         CHAR(32)      NOT NULL COMMENT '商品ID',
    `user_id`          CHAR(32)      NOT NULL COMMENT '用户ID',

    -- 订单信息
    `buy_count`        INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    `amount`           DECIMAL(10,2) NOT NULL COMMENT '订单金额 = 秒杀价 × 购买数量',
    `status`           TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已取消',

    -- 时间戳
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_user_goods` (`user_id`, `goods_id`) COMMENT '防重索引：同一用户对同一商品只能下单一次',
    KEY `idx_order_goods_id` (`goods_id`) COMMENT '按商品查询订单'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单事件消息去重表（RocketMQ 异步落单幂等性保障）
CREATE TABLE IF NOT EXISTS `t_order_message_processed` (
    -- 核心标识
    `id`           CHAR(32)        NOT NULL COMMENT '记录ID，32位无横杠UUIDv7格式',

    -- 消息信息
    `message_id`   VARCHAR(64)     NOT NULL COMMENT 'RocketMQ 消息唯一 ID',
    `business_key` VARCHAR(100)    NOT NULL COMMENT '业务唯一键，格式：{userId}#{goodsId}',

    -- 时间戳
    `processed_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息处理时间',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`) COMMENT '消息ID唯一索引，防止重复处理',
    KEY `idx_business_key` (`business_key`) COMMENT '业务键索引',
    KEY `idx_processed_at` (`processed_at`) COMMENT '处理时间索引，用于清理历史数据'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单事件消息已处理记录表';

-- 说明：
-- 1. t_order 表通过唯一索引 uk_order_user_goods 防止重复下单（第3层防重）
-- 2. t_order_message_processed 表用于 RocketMQ 消费端幂等性检查（第2层防重）
-- 3. 第1层防重：Redis 分布式锁（order:processing:{businessKey}）
-- 4. 三层防重机制确保在高并发和消息重投场景下不会重复创建订单
