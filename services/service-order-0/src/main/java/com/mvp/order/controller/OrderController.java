package com.mvp.order.controller;

import com.mvp.common.vo.ResultVO;
import com.mvp.order.dto.GoodsInfoDto;
import com.mvp.order.dto.OrderRequestDto;
import com.mvp.order.dto.OrderResultDto;
import com.mvp.order.feign.GoodsStockClient;
import com.mvp.order.service.OrderProducerService;
import com.mvp.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Objects;

/**
 * 订单控制器。
 *
 * <p>暴露下单和结果查询两个核心接口。用户 ID 由网关鉴权后通过 {@code X-User-Id} 透传，
 * 本服务无需自己解析 token。</p>
 *
 * <p>异步改造说明：
 * <ul>
 *   <li>/order/submit 改为异步模式：快速校验后发送消息到 RocketMQ，立即返回"排队中"</li>
 *   <li>/order/result 保持不变：前端轮询查询最终结果</li>
 * </ul>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderProducerService producerService;
    private final GoodsStockClient goodsStockClient;

    public OrderController(OrderService orderService,
                          OrderProducerService producerService,
                          GoodsStockClient goodsStockClient) {
        this.orderService = orderService;
        this.producerService = producerService;
        this.goodsStockClient = goodsStockClient;
    }

    /**
     * 发起秒杀（异步模式）。
     *
     * <p>处理流程：
     * <ol>
     *   <li>快速校验：商品存在性、启用状态、时间窗口、限购</li>
     *   <li>第一层防重：Redis 用户标记 + 数据库订单查询</li>
     *   <li>发送消息到 RocketMQ order-events topic</li>
     *   <li>立即返回 status=0（排队中），不等待落库完成</li>
     *   <li>前端轮询 /order/result 获取最终结果</li>
     * </ol>
     *
     * @param userId     网关透传的当前登录用户 ID
     * @param requestDto 秒杀请求参数，包含商品 ID 和购买数量
     * @return 秒杀结果（异步模式下返回"排队中"）
     */
    @PostMapping("/submit")
    public ResultVO<OrderResultDto> submit(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String userId,
                                           @Valid @RequestBody OrderRequestDto requestDto) {
        String goodsId = requestDto.getGoodsId();
        int buyCount = requestDto.getBuyCount() != null ? requestDto.getBuyCount() : 1;

        try {
            // 第1步：查询商品快照并快速校验
            GoodsInfoDto goods = loadAndValidateGoods(goodsId, buyCount);

            // 第2步：第一层防重检查（Redis + 数据库）
            OrderResultDto existingResult = orderService.queryResult(userId, goodsId);
            if (existingResult.getStatus() == OrderResultDto.STATUS_SUCCESS) {
                return ResultVO.ok(existingResult);
            }
            if (existingResult.getStatus() == OrderResultDto.STATUS_FAIL) {
                return ResultVO.ok(existingResult);
            }

            // 第3步：发送消息到 RocketMQ
            boolean sent = producerService.sendOrderEvent(userId, requestDto, goods);

            if (!sent) {
                // 发送失败，降级到同步模式
                log.warn("消息发送失败，降级到同步处理 userId={} goodsId={}", userId, goodsId);
                return ResultVO.ok(orderService.doSeckill(userId, requestDto));
            }

            // 第4步：立即返回"排队中"
            OrderResultDto result = new OrderResultDto();
            result.setStatus(OrderResultDto.STATUS_QUEUEING);
            result.setMessage("排队中，请稍后查询结果");

            log.info("秒杀请求已提交 userId={} goodsId={} buyCount={}", userId, goodsId, buyCount);

            return ResultVO.ok(result);

        } catch (IllegalArgumentException ex) {
            // 业务校验失败，直接返回失败结果
            log.warn("秒杀请求校验失败 userId={} goodsId={} reason={}", userId, goodsId, ex.getMessage());
            OrderResultDto result = new OrderResultDto();
            result.setStatus(OrderResultDto.STATUS_FAIL);
            result.setMessage(ex.getMessage());
            return ResultVO.ok(result);

        } catch (Exception ex) {
            log.error("秒杀请求处理异常 userId={} goodsId={}", userId, goodsId, ex);
            return ResultVO.fail("系统异常，请稍后重试");
        }
    }

    /**
     * 快速校验商品信息
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
     * 查询秒杀结果。
     *
     * @param userId  网关透传的当前登录用户 ID
     * @param goodsId 商品 ID
     * @return 当前用户对该商品的秒杀结果
     */
    @GetMapping("/result")
    public ResultVO<OrderResultDto> result(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String userId,
                                           @RequestParam String goodsId) {
        return ResultVO.ok(orderService.queryResult(userId, goodsId));
    }
}
