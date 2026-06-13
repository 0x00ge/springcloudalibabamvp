package com.mvp.goods.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀商品信息 DTO（goods 侧）。
 *
 * <p>goods 服务把商品配置组装成本类对外输出，供 order 服务通过 Feign 在下单前完成
 * 启用状态、秒杀时间窗口、限购数量、秒杀价等校验。</p>
 *
 * <p>order 服务持有同结构的另一份副本。两边各自独立，靠 Feign 的 JSON 字段名匹配完成序列化，
 * 这样 goods 和 order 不必通过共享模块产生编译期耦合。</p>
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
