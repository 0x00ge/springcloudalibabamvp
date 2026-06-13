package com.mvp.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.order.dto.OrderResultDto;
import com.mvp.order.dto.OrderRequestDto;
import com.mvp.order.entity.Order;

/**
 * 订单 Service。
 *
 * <p>除复用 MyBatis-Plus 通用 CRUD 外，还承载秒杀下单和结果查询两个核心业务能力。</p>
 */
public interface OrderService extends IService<Order> {

    /**
     * 发起秒杀。
     *
     * @param userId     网关透传的用户 ID
     * @param requestDto 秒杀请求参数
     * @return 秒杀结果
     */
    OrderResultDto doSeckill(String userId, OrderRequestDto requestDto);

    /**
     * 查询秒杀结果。
     *
     * @param userId  用户 ID
     * @param goodsId 商品 ID
     * @return 秒杀结果
     */
    OrderResultDto queryResult(String userId, String goodsId);
}
