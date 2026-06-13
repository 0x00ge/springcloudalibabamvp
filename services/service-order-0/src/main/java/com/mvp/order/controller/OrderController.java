package com.mvp.order.controller;

import com.mvp.order.dto.OrderResultDto;
import com.mvp.common.vo.ResultVO;
import com.mvp.order.dto.OrderRequestDto;
import com.mvp.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器。
 *
 * <p>暴露下单和结果查询两个核心接口。用户 ID 由网关鉴权后通过 {@code X-User-Id} 透传，
 * 本服务无需自己解析 token。</p>
 */
@Validated
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 发起秒杀。
     *
     * @param userId     网关透传的当前登录用户 ID
     * @param requestDto 秒杀请求参数，包含商品 ID 和购买数量
     * @return 秒杀结果
     */
    @PostMapping("/submit")
    public ResultVO<OrderResultDto> submit(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String userId,
                                           @Valid @RequestBody OrderRequestDto requestDto) {
        return ResultVO.ok(orderService.doSeckill(userId, requestDto));
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
