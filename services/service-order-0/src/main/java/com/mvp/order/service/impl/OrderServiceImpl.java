package com.mvp.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderResultDto;
import com.mvp.common.vo.ResultVO;
import com.mvp.order.dto.OrderRequestDto;
import com.mvp.order.entity.Order;
import com.mvp.order.feign.GoodsStockClient;
import com.mvp.order.mapper.OrderMapper;
import com.mvp.order.service.OrderService;
import com.mvp.order.service.OrderTxService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 订单 Service 实现。
 *
 * <p>这是拆分后的订单服务核心，主流程编排沿用了 service-seckill-local-0 的思路，
 * 区别在于：库存不再由本服务直接扣减，而是通过 Feign 调用 service-goods-0 完成预扣和回补。</p>
 *
 * <p>本服务自己负责：用户防重标记、用户已有订单校验、订单落库（独立事务 Bean）、结果缓存和失败补偿。</p>
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

    /**
     * Redis 用户秒杀标记 key 前缀：{@code seckill:user:{userId}:{goodsId}}。
     */
    private static final String USER_MARK_KEY_PREFIX = "seckill:user:";

    /**
     * Redis 秒杀结果缓存 key 前缀：{@code seckill:result:{userId}:{goodsId}}。
     */
    private static final String RESULT_KEY_PREFIX = "seckill:result:";

    /**
     * 商品服务 Feign 客户端，负责库存预扣、回补和商品快照查询。
     */
    private final GoodsStockClient goodsStockClient;

    /**
     * 订单事务 Service，负责订单落库的本地事务。
     */
    private final OrderTxService orderTxService;

    /**
     * Redisson 客户端，负责用户防重标记和结果缓存。
     */
    private final RedissonClient redissonClient;

    public OrderServiceImpl(GoodsStockClient goodsStockClient,
                            OrderTxService orderTxService,
                            RedissonClient redissonClient) {
        this.goodsStockClient = goodsStockClient;
        this.orderTxService = orderTxService;
        this.redissonClient = redissonClient;
    }

    /**
     * 发起秒杀。
     *
     * <p>处理顺序：</p>
     * <p>1. 通过 Feign 查商品快照并校验启用状态、时间窗口、限购；</p>
     * <p>2. Redis 用户标记 + 数据库订单双重防重；</p>
     * <p>3. 通过 Feign 预扣库存；</p>
     * <p>4. 抢用户标记，失败则回补库存；</p>
     * <p>5. 独立事务 Bean 落订单；</p>
     * <p>6. 落库失败时回补库存、删标记、缓存失败结果。</p>
     */
    @Override
    public OrderResultDto doSeckill(String userId, OrderRequestDto requestDto) {
        String goodsId = requestDto.getGoodsId();
        int buyCount = requestDto.getBuyCount() == null ? 1 : requestDto.getBuyCount();

        // 第 1 步：查商品快照并校验。
        GoodsInfoDto goods = loadAndValidateGoods(goodsId, buyCount);

        // 第 2 步：Redis 用户标记快速挡重复请求。
        String userMarkKey = userMarkKey(userId, goodsId);
        if (redissonClient.getBucket(userMarkKey).isExists()) {
            return failResult("请勿重复秒杀");
        }

        // 第 3 步：数据库订单兜底防重，覆盖缓存失效、服务重启等场景。
        long orderCount = count(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getGoodsId, goodsId));
        if (orderCount > 0) {
            return failResult("您已秒杀成功，请勿重复下单");
        }

        // 第 4 步：通过 Feign 预扣库存。库存权威在 goods 服务。
        // 注意顺序：这里先扣库存、再抢用户标记（第 5 步）。极端并发下同一用户两个请求可能都通过了
        // 第 2/3 步的防重检查，于是都来扣一次库存，随后只有一个能抢到用户标记，另一个会在第 5 步回补。
        // 这是一个已知的小竞态窗口，最终不会超卖（库存原子扣减 + 订单唯一索引兜底），但会短暂多占一个名额。
        if (!deductStock(goodsId, buyCount)) {
            return failResult("商品已售罄");
        }

        // 第 5 步：抢用户标记。trySet 仅首次成功，失败说明并发下已有请求占位，
        // 必须把第 4 步刚预扣的库存补回来，否则失败请求会平白吃掉一个库存名额。
        boolean marked = tryMarkUser(userMarkKey, resultTtl(goods));
        if (!marked) {
            rollbackStock(goodsId, buyCount);
            return failResult("请勿重复秒杀");
        }

        // 第 6 步：落正式订单（独立事务 Bean，避免自调用导致事务失效）。
        try {
            OrderResultDto result = orderTxService.createOrder(userId, goods, buyCount);
            cacheResult(userId, goodsId, result);
            log.info("秒杀成功 userId={} goodsId={} buyCount={} orderId={}",
                    userId, goodsId, buyCount, result.getOrderId());
            return result;
        } catch (Exception ex) {
            // 第 7 步：落库失败补偿——回补库存、删标记、缓存失败结果。
            compensateOnFailure(userMarkKey, goodsId, buyCount, userId, ex);
            throw ex;
        }
    }

    /**
     * 查询秒杀结果。优先读 Redis 缓存，缓存没有时查订单表。
     */
    @Override
    public OrderResultDto queryResult(String userId, String goodsId) {
        // 第 1 层：Redis 结果缓存，适合前端轮询。
        String resultCache = (String) redissonClient.getBucket(resultKey(userId, goodsId)).get();
        if (StringUtils.hasText(resultCache)) {
            return decodeResult(resultCache);
        }

        // 第 2 层：查订单表，命中即说明已成功落库。
        Order order = getOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getGoodsId, goodsId)
                .last("limit 1"), false);
        if (order != null) {
            OrderResultDto result = new OrderResultDto();
            result.setStatus(OrderResultDto.STATUS_SUCCESS);
            result.setOrderId(order.getId());
            result.setMessage("秒杀成功");
            return result;
        }

        // 第 3 层：都没命中，返回排队中。
        OrderResultDto result = new OrderResultDto();
        result.setStatus(OrderResultDto.STATUS_QUEUEING);
        result.setMessage("排队中");
        return result;
    }

    /**
     * 查询并校验商品。
     *
     * <p>校验项：商品存在、状态启用、当前时间在秒杀时间窗口内、购买数量不超过限购。
     * 商品快照通过 Feign 从 goods 服务获取。</p>
     */
    private GoodsInfoDto loadAndValidateGoods(String goodsId, int buyCount) {
        ResultVO<GoodsInfoDto> resp = goodsStockClient.info(goodsId);
        GoodsInfoDto goods = resp == null ? null : resp.getData();
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!Objects.equals(goods.getStatus(), GoodsInfoDto.STATUS_ENABLED)) {
            throw new IllegalArgumentException("商品未启用");
        }

        Date now = new Date();
        if (goods.getStartTime() != null && now.before(goods.getStartTime())) {
            throw new IllegalArgumentException("秒杀尚未开始");
        }
        if (goods.getEndTime() != null && now.after(goods.getEndTime())) {
            throw new IllegalArgumentException("秒杀已结束");
        }
        if (goods.getLimitPerUser() != null && buyCount > goods.getLimitPerUser()) {
            throw new IllegalArgumentException("超过限购数量");
        }
        return goods;
    }

    /**
     * 通过 Feign 预扣库存。
     *
     * <p>把远程调用结果统一收敛成布尔值，调用失败或返回非成功都按预扣失败处理。</p>
     */
    private boolean deductStock(String goodsId, int buyCount) {
        ResultVO<Boolean> resp = goodsStockClient.deduct(goodsId, buyCount);
        return resp != null && resp.isSuccess() && Boolean.TRUE.equals(resp.getData());
    }

    /**
     * 通过 Feign 回补库存。
     *
     * <p>回补失败只记录告警，不再抛出，避免补偿过程把原始异常掩盖掉。
     * 最小实现暂不做二次补偿，后续可用 MQ 或定时对账增强。</p>
     */
    private void rollbackStock(String goodsId, int buyCount) {
        try {
            goodsStockClient.rollback(goodsId, buyCount);
        } catch (Exception ex) {
            log.warn("回补库存失败 goodsId={} buyCount={} reason={}", goodsId, buyCount, ex.getMessage());
        }
    }

    /**
     * 为用户设置秒杀处理中标记，仅首次成功。
     */
    private boolean tryMarkUser(String userMarkKey, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(userMarkKey);
        return bucket.trySet("1", ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 下单失败补偿：回补库存、删用户标记、缓存失败结果。
     *
     * <p>由于 Feign 库存预扣和数据库订单事务不在同一边界内，落库失败必须手动回滚远程库存和本地缓存状态。</p>
     */
    private void compensateOnFailure(String userMarkKey, String goodsId, int buyCount, String userId, Exception ex) {
        redissonClient.getBucket(userMarkKey).delete();
        rollbackStock(goodsId, buyCount);
        cacheResult(userId, goodsId, failResult(ex.getMessage()));
        log.warn("秒杀失败并已补偿 userId={} goodsId={} reason={}", userId, goodsId, ex.getMessage());
    }

    /**
     * 缓存秒杀结果，默认 2 小时，覆盖前端轮询窗口。
     */
    private void cacheResult(String userId, String goodsId, OrderResultDto result) {
        redissonClient.getBucket(resultKey(userId, goodsId)).set(encodeResult(result), Duration.ofHours(2));
    }

    /**
     * 组装失败结果，无明确信息时使用兜底文案。
     */
    private OrderResultDto failResult(String message) {
        OrderResultDto result = new OrderResultDto();
        result.setStatus(OrderResultDto.STATUS_FAIL);
        result.setMessage(StringUtils.hasText(message) ? message : "秒杀失败");
        return result;
    }

    /**
     * 生成用户秒杀标记 key。
     */
    private String userMarkKey(String userId, String goodsId) {
        return USER_MARK_KEY_PREFIX + userId + ":" + goodsId;
    }

    /**
     * 生成结果缓存 key。
     */
    private String resultKey(String userId, String goodsId) {
        return RESULT_KEY_PREFIX + userId + ":" + goodsId;
    }

    /**
     * 计算用户标记和结果缓存的 TTL。
     *
     * <p>若商品设置了秒杀结束时间，则至少缓存到结束；否则最少保留 2 小时。</p>
     */
    private Duration resultTtl(GoodsInfoDto goods) {
        if (goods.getEndTime() == null) {
            return Duration.ofHours(2);
        }
        long millis = Math.max(Duration.ofHours(2).toMillis(), goods.getEndTime().getTime() - System.currentTimeMillis());
        return Duration.ofMillis(millis);
    }

    /**
     * 把结果编码成竖线分隔字符串存入 Redis，避免额外引入 JSON 序列化配置。
     *
     * <p>编码格式固定为 {@code status|requestNo|orderId|message} 四段，与 {@link #decodeResult(String)} 一一对应。
     * 用竖线拼接而不是 JSON，是为了在最小实现里省掉 RedisTemplate 的序列化器配置，保持依赖最少。
     * 字段为 null 时先转成空串占位，保证段数稳定，反解时再还原成 null。</p>
     */
    private String encodeResult(OrderResultDto result) {
        return String.join("|",
                String.valueOf(result.getStatus()),
                defaultString(result.getRequestNo()),
                defaultString(result.getOrderId()),
                defaultString(result.getMessage()));
    }

    /**
     * 把 Redis 字符串结果反解成 DTO。
     *
     * <p>{@code split("\\|", -1)} 的第二个参数为 -1，表示保留末尾的空字符串，
     * 否则当 message 等尾部字段为空时会被丢弃，导致段数不足、解析错位。</p>
     */
    private OrderResultDto decodeResult(String value) {
        String[] parts = value.split("\\|", -1);
        OrderResultDto result = new OrderResultDto();
        result.setStatus(Integer.parseInt(parts[0]));
        result.setRequestNo(emptyToNull(parts.length > 1 ? parts[1] : null));
        result.setOrderId(emptyToNull(parts.length > 2 ? parts[2] : null));
        result.setMessage(emptyToNull(parts.length > 3 ? parts[3] : null));
        return result;
    }

    /**
     * null 转空字符串，便于统一拼接。
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 空字符串转 null，便于还原 DTO 语义。
     */
    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
