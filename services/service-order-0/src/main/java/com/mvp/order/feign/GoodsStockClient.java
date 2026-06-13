package com.mvp.order.feign;

import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.common.vo.ResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端。
 *
 * <p>order 服务通过本客户端调用 service-goods-0 的内部库存接口，完成商品快照查询、库存预扣和回补。</p>
 *
 * <p>库存权威完全在 goods 服务，order 服务不直接操作 Redis 库存 key，
 * 只通过这里的远程调用参与库存变更，保证库存账目集中可控。</p>
 */
@FeignClient(name = "service-goods-0", path = "/goods/stock")
public interface GoodsStockClient {

    /**
     * 查询商品快照。
     *
     * @param id 秒杀商品 ID
     * @return 商品快照；商品不存在时 data 为 null
     */
    @GetMapping("/{id}/info")
    ResultVO<GoodsInfoDto> info(@PathVariable("id") String id);

    /**
     * 预扣库存。
     *
     * @param id    秒杀商品 ID
     * @param count 扣减数量
     * @return data 为 true 表示扣减成功，false 表示库存不足
     */
    @PostMapping("/{id}/deduct")
    ResultVO<Boolean> deduct(@PathVariable("id") String id, @RequestParam("count") int count);

    /**
     * 回补库存。
     *
     * @param id    秒杀商品 ID
     * @param count 回补数量
     * @return 统一返回结果
     */
    @PostMapping("/{id}/rollback")
    ResultVO<Void> rollback(@PathVariable("id") String id, @RequestParam("count") int count);
}
