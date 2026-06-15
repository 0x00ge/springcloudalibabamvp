package com.mvp.order.service;

import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderRequestDto;

/**
 * 订单消息生产者服务接口
 *
 * <p>负责将秒杀下单请求转换为消息事件并发送到 RocketMQ，
 * 实现异步削峰和解耦。
 */
public interface OrderProducerService {

    /**
     * 发送订单事件消息到 RocketMQ
     *
     * <p>异步下单流程的第一步：
     * <ol>
     *   <li>构建 OrderEventMessage（包含用户ID、商品快照、购买数量）</li>
     *   <li>发送到 order-events:order.placed topic</li>
     *   <li>消费者异步处理（库存扣减 + 订单落库）</li>
     * </ol>
     *
     * @param userId     用户ID（32位无横杠 UUIDv7）
     * @param requestDto 下单请求参数
     * @param goods      商品快照（下单时的商品信息）
     * @return true=发送成功，false=发送失败
     */
    boolean sendOrderEvent(String userId, OrderRequestDto requestDto, GoodsInfoDto goods);
}
