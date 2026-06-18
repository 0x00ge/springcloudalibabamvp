package com.mvp.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类。
 *
 * <p>订单服务负责秒杀下单主流程和结果查询，库存操作通过 Feign 委托给 service-goods-0。</p>
 *
 * <p>四个核心注解的作用：</p>
 * <ul>
 *   <li>{@code @EnableFeignClients(basePackages = "com.mvp.order.feign")}：开启 Feign，扫描该包下的
 *       {@code @FeignClient} 接口（如 {@code GoodsStockClient}）并生成远程调用代理。</li>
 *   <li>{@code @MapperScan("com.mvp.order.mapper")}：扫描并注册 MyBatis-Plus 的 {@code OrderMapper}。</li>
 *   <li>{@code @SpringBootApplication}：开启自动装配和组件扫描。</li>
 *   <li>{@code scanBasePackages} 额外纳入 {@code com.mvp.common}：让公共模块的 UUIDv7 主键生成器等 Bean 生效。</li>
 * </ul>
 */
@EnableFeignClients(basePackages = "com.mvp.order.feign")
@MapperScan("com.mvp.order.mapper")
@SpringBootApplication(scanBasePackages = {"com.mvp.order", "com.mvp.common"})
public class OrderApplication {

    /**
     * 服务入口。
     *
     * <p>启动后向 Nacos 注册（服务名 service-order-0），对外提供 {@code /order/submit} 下单和
     * {@code /order/result} 结果查询接口；下单时通过 Feign 调用 service-goods-0 完成库存预扣与回补。</p>
     */
    public static void main(String[] args) {
        System.setProperty("com.rocketmq.sendMessageWithVIPChannel", "false");
        org.springframework.boot.SpringApplication.run(OrderApplication.class, args);
    }
}
