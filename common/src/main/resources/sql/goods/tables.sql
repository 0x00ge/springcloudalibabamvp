-- ============================================
-- service-goods-0 数据库表结构
-- ============================================
-- 商品服务负责商品配置、库存管理
-- 依赖表：t_goods
-- ============================================

CREATE TABLE IF NOT EXISTS `t_goods` (
    -- 核心标识
    `id`              CHAR(32)      NOT NULL COMMENT '商品ID，32位无横杠UUIDv7格式',

    -- 商品信息
    `name`            VARCHAR(200)  NOT NULL COMMENT '商品名称',
    `seckill_price`   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',

    -- 库存控制
    `total_stock`     INT           NOT NULL COMMENT '总库存（仅作Redis初始化种子）',
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
    KEY `idx_goods_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 说明：
-- 1. total_stock 仅作 Redis 库存初始化的种子值，真实剩余库存由 Redis 原子计数维护
-- 2. 库存 key: seckill:stock:{goodsId}
-- 3. 商品服务是库存的唯一权威方，订单服务通过 Feign 调用完成库存预扣和回补
