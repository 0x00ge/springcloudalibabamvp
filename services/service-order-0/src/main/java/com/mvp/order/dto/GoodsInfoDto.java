package com.mvp.order.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀商品信息 DTO（order 侧副本）。
 *
 * <p>order 服务通过 Feign 调用 service-goods-0 时，用本类承接商品快照，
 * 完成下单前的启用状态、秒杀时间窗口、限购数量、秒杀价等校验。</p>
 *
 * <p>goods 服务持有同结构的另一份 DTO。两边各自独立，靠 Feign 的 JSON 字段名匹配完成反序列化，
 * 这样 order 和 goods 不必通过共享模块产生编译期耦合。</p>
 */
@Data
public class GoodsInfoDto implements Serializable {

    /**
     * 商品状态：禁用。
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 商品状态：启用。
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 秒杀商品 ID。
     */
    private String id;

    /**
     * 商品名称。
     */
    private String name;

    /**
     * 秒杀价。
     */
    private BigDecimal seckillPrice;

    /**
     * 总库存。
     *
     * <p>仅作展示和 Redis 库存初始化参考，真实剩余库存以 Redis 原子计数为准。</p>
     */
    private Integer totalStock;

    /**
     * 每人限购数量。
     */
    private Integer limitPerUser;

    /**
     * 秒杀开始时间。
     */
    private Date startTime;

    /**
     * 秒杀结束时间。
     */
    private Date endTime;

    /**
     * 商品状态：0-禁用，1-启用。
     */
    private Integer status;

    @Serial
    private static final long serialVersionUID = 1L;
}
