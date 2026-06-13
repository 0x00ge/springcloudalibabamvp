package com.mvp.goods.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.goods.entity.Goods;
import com.mvp.goods.mapper.GoodsMapper;
import com.mvp.goods.service.GoodsService;
import com.mvp.goods.dto.GoodsInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 商品 Service 实现。
 *
 * <p>商品配置走数据库通用 CRUD；库存计数走 Redis，由本服务作为唯一权威方维护。</p>
 *
 * <p>这样设计的目的是：把高并发的库存扣减集中到一个服务里，
 * order 服务只通过 Feign 调用本服务完成预扣和回补，不直接操作库存 key，
 * 避免库存权威分散在多个服务导致难以对账。</p>
 */
@Slf4j
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods>
        implements GoodsService {

    /**
     * Redis 秒杀库存 key 前缀。
     *
     * <p>格式：{@code seckill:stock:{goodsId}}。精简版商品表自带时间窗口，
     * 不再有独立活动维度，所以库存 key 只按商品 ID 区分。</p>
     */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /**
     * Redisson 客户端，用于库存原子计数。
     */
    private final RedissonClient redissonClient;

    public GoodsServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 查询商品快照。
     *
     * <p>只做存在性判断，启用状态、时间窗口、限购等业务校验交给 order 服务，
     * 这样商品服务保持纯粹的数据提供方角色。</p>
     */
    @Override
    public GoodsInfoDto getGoodsInfo(String id) {
        Goods goods = getById(id);
        if (goods == null) {
            return null;
        }
        GoodsInfoDto dto = new GoodsInfoDto();
        BeanUtils.copyProperties(goods, dto);
        return dto;
    }

    /**
     * 预扣库存。
     *
     * <p>逻辑分两段：</p>
     * <p>1. 库存 key 不存在时，用数据库 {@code total_stock} 懒加载初始化；</p>
     * <p>2. 用 {@code addAndGet(-count)} 原子扣减，扣减后小于 0 则立即回补并判定失败。</p>
     *
     * <p>这里是最小实现的懒加载，高并发冷启动存在初始化竞争，生产版建议活动开始前预热。</p>
     */
    @Override
    public boolean deductStock(String id, int count) {
        // 防御性校验：扣减数量必须为正，避免传入 0 或负数把库存“扣”成增加。
        if (count <= 0) {
            return false;
        }
        // 商品必须存在才有扣减意义；同时这一步也为下面的库存懒加载提供 total_stock 种子值。
        Goods goods = getById(id);
        if (goods == null) {
            return false;
        }

        RAtomicLong atomicStock = redissonClient.getAtomicLong(stockKey(id));
        if (!atomicStock.isExists()) {
            // compareAndSet(0, seed) 保证只有第一个请求能完成初始化：
            // 期望值为 0（key 不存在时 RAtomicLong 读出来就是 0），命中才写入种子。
            // 其余并发请求即使同时进来，期望值已不再是 0，不会覆盖已有计数，从而避免库存被重置。
            long seed = goods.getTotalStock() == null ? 0L : goods.getTotalStock().longValue();
            atomicStock.compareAndSet(0L, seed);
            // 给库存 key 设置 1 天过期，避免活动结束后残留 key 长期占用内存。
            atomicStock.expire(Duration.ofDays(1));
        }

        // addAndGet(-count) 是原子操作，并发下不会出现两个请求读到同一剩余值的竞态，这是防超卖的第一道防线。
        long left = atomicStock.addAndGet(-count);
        if (left < 0) {
            // 扣过头了（库存已售罄），把刚扣的数量补回去，
            // 确保计数不会因为失败请求持续往负数累积，否则后续回补会算错基数。
            atomicStock.addAndGet(count);
            log.info("库存不足 goodsId={} count={} left={}", id, count, left);
            return false;
        }
        log.info("预扣库存成功 goodsId={} count={} left={}", id, count, left);
        return true;
    }

    /**
     * 回补库存。
     *
     * <p>下单失败时把预扣的数量加回 Redis 计数。这里不判断 key 是否存在，
     * 因为正常流程下能触发回补，说明之前一定预扣过，key 必然存在。</p>
     */
    @Override
    public void rollbackStock(String id, int count) {
        if (count <= 0) {
            return;
        }
        redissonClient.getAtomicLong(stockKey(id)).addAndGet(count);
        log.info("回补库存 goodsId={} count={}", id, count);
    }

    /**
     * 生成 Redis 库存 key。
     */
    private String stockKey(String id) {
        return STOCK_KEY_PREFIX + id;
    }
}
