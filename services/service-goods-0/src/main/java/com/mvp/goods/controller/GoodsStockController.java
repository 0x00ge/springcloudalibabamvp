package com.mvp.goods.controller;

import com.mvp.goods.service.GoodsService;
import com.mvp.goods.dto.GoodsInfoDto;
import com.mvp.common.vo.ResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品内部接口控制器。
 *
 * <p>这些接口主要面向 service-order-0 通过 Feign 调用，承担两类职责：</p>
 * <p>1. 提供商品快照，供下单前校验；</p>
 * <p>2. 作为库存权威方，对外暴露库存预扣和回补。</p>
 *
 * <p>路径放在 {@code /goods/stock} 下，和管理端 CRUD 的 {@code /goods} 区分开。
 * 当前最小实现没有给内部接口单独做鉴权隔离，生产环境建议通过内网或专用网关限制访问来源。</p>
 */
@RestController
@RequestMapping("/goods/stock")
public class GoodsStockController {

    private final GoodsService goodsService;

    public GoodsStockController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    /**
     * 查询商品快照。
     *
     * @param id 商品 ID
     * @return 商品快照；商品不存在时返回失败
     */
    @GetMapping("/{id}/info")
    public ResultVO<GoodsInfoDto> info(@PathVariable String id) {
        GoodsInfoDto info = goodsService.getGoodsInfo(id);
        if (info == null) {
            return ResultVO.fail("商品不存在");
        }
        return ResultVO.ok(info);
    }

    /**
     * 预扣库存。
     *
     * <p>由 order 服务在下单主流程中通过 Feign 调用。库存权威在本服务的 Redis 计数里，
     * 扣减是原子操作，返回 false 表示已售罄。order 服务据此决定是否继续后续落单步骤。</p>
     *
     * @param id    商品 ID
     * @param count 扣减数量，默认 1
     * @return data 为 true 表示扣减成功，false 表示库存不足
     */
    @PostMapping("/{id}/deduct")
    public ResultVO<Boolean> deduct(@PathVariable String id,
                                    @RequestParam(defaultValue = "1") int count) {
        return ResultVO.ok(goodsService.deductStock(id, count));
    }

    /**
     * 回补库存。
     *
     * <p>由 order 服务在下单失败补偿时通过 Feign 调用，把之前预扣成功但最终没落单的数量加回。
     * 这是 Redis 库存与数据库订单之间最终一致的关键一环：预扣和落单不在同一事务，
     * 落单失败就必须显式回补，否则库存会平白少掉。</p>
     *
     * @param id    商品 ID
     * @param count 回补数量，默认 1
     * @return 始终返回成功
     */
    @PostMapping("/{id}/rollback")
    public ResultVO<Void> rollback(@PathVariable String id,
                                   @RequestParam(defaultValue = "1") int count) {
        goodsService.rollbackStock(id, count);
        return ResultVO.ok();
    }
}
