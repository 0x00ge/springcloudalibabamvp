package com.mvp.order.service.impl;

import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderResultDto;
import com.mvp.order.entity.Order;
import com.mvp.order.mapper.OrderMapper;
import com.mvp.order.service.OrderTxService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 订单事务 Service 实现。
 *
 * <p>承接真正需要事务保护的订单落库操作。单独拆成 Bean 是为了避免在同一个类内自调用
 * {@code @Transactional} 导致事务代理失效。</p>
 *
 * <p>注意：本类只依赖 {@link OrderMapper} 落库，不注入 {@code OrderService}。
 * 若注入 OrderService，会与 {@code OrderServiceImpl → OrderTxService} 形成循环依赖，
 * 在 Spring Boot 2.6+ 默认禁止循环引用时导致启动失败。</p>
 */
@Service
public class OrderTxServiceImpl implements OrderTxService {

    /**
     * 订单 Mapper，直接写库，避免再依赖 OrderService 造成 Bean 环。
     */
    private final OrderMapper orderMapper;

    public OrderTxServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 创建正式订单。
     *
     * <p>精简版没有请求日志表，事务里只做订单落库一件事：</p>
     * <p>1. 组装订单，金额 = 秒杀价 × 购买数量；</p>
     * <p>2. 保存订单，依赖 {@code uk_order_user_goods} 唯一索引兜底防重；</p>
     * <p>3. 唯一索引冲突时抛出业务异常，触发事务回滚和外层库存回补。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResultDto createOrder(String userId, GoodsInfoDto goods, int buyCount) {
        Order order = new Order();
        order.setGoodsId(goods.getId());
        order.setUserId(userId);
        order.setBuyCount(buyCount);
        order.setAmount(goods.getSeckillPrice().multiply(BigDecimal.valueOf(buyCount)));
        order.setStatus(Order.STATUS_PENDING_PAY);

        try {
            // 使用 Mapper 插入；MyBatis-Plus 会按实体主键策略生成 UUIDv7
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            // 唯一索引兜底：同一用户对同一商品只能成功下单一次。
            throw new IllegalArgumentException("您已秒杀成功，请勿重复下单", ex);
        }

        OrderResultDto result = new OrderResultDto();
        result.setStatus(OrderResultDto.STATUS_SUCCESS);
        result.setOrderId(order.getId());
        result.setMessage("秒杀成功");
        return result;
    }
}
