package com.mvp.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.goods.entity.Goods;
import com.mvp.goods.dto.GoodsInfoDto;

/**
 * 商品 Service。
 *
 * <p>除了复用 MyBatis-Plus 的通用 CRUD 能力维护商品配置外，还额外承担两类对内能力：</p>
 * <p>1. 把商品配置组装成 {@link GoodsInfoDto} 快照，供 order 服务下单前校验；</p>
 * <p>2. 作为库存权威方，对外暴露库存预扣和回补能力（实际计数落在 Redis）。</p>
 */
public interface GoodsService extends IService<Goods> {

    /**
     * 查询商品快照，供 order 服务下单前校验使用。
     *
     * @param id 商品 ID
     * @return 商品快照，不存在时返回 null
     */
    GoodsInfoDto getGoodsInfo(String id);

    /**
     * 预扣库存。
     *
     * <p>基于 Redis 原子计数扣减指定数量，返回是否扣减成功。
     * 库存不足时不会把计数扣成负数。</p>
     *
     * @param id    商品 ID
     * @param count 扣减数量
     * @return true-扣减成功，false-库存不足
     */
    boolean deductStock(String id, int count);

    /**
     * 回补库存。
     *
     * <p>下单失败时由 order 服务回调，把之前预扣的数量加回 Redis 计数。</p>
     *
     * @param id    商品 ID
     * @param count 回补数量
     */
    void rollbackStock(String id, int count);
}
