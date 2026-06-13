package com.mvp.order.service;

import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderResultDto;

/**
 * 订单事务 Service。
 *
 * <p>把真正需要事务保护的订单落库操作单独拆成一个 Bean，
 * 通过外部 Bean 调用触发 Spring 事务代理，避免同类自调用导致 {@code @Transactional} 失效。</p>
 */
public interface OrderTxService {

    /**
     * 创建正式订单。
     *
     * <p>在数据库本地事务里完成订单落库，依赖订单表唯一索引兜底防重。
     * 库存扣减不在这里做，已经在 order 服务调用 goods 服务 Feign 预扣阶段完成。</p>
     *
     * @param userId   下单用户 ID
     * @param goods    商品快照
     * @param buyCount 购买数量
     * @return 秒杀结果
     */
    OrderResultDto createOrder(String userId, GoodsInfoDto goods, int buyCount);
}
