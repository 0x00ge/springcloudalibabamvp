package com.mvp.goods;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商品服务启动类。
 *
 * <p>商品服务从秒杀链路中拆出，专注于商品配置维护和库存权威管理，
 * 便于库存扣减能力单独扩容和演进。</p>
 *
 * <p>三个核心注解的作用：</p>
 * <ul>
 *   <li>{@code @MapperScan("com.mvp.goods.mapper")}：扫描本服务的 MyBatis-Plus Mapper 接口并注册为 Bean，
 *       这样 {@code GoodsMapper} 无需逐个标注 {@code @Mapper}。</li>
 *   <li>{@code @SpringBootApplication}：开启自动装配和组件扫描。</li>
 *   <li>{@code scanBasePackages} 额外纳入 {@code com.mvp.common}：让公共模块里的 Bean（如 UUIDv7 主键生成器
 *       {@code UuidV7IdentifierGenerator}）能被本服务扫描到。common 模块里的分页拦截器等能力则是通过
 *       Spring Boot 自动装配机制生效的，不依赖组件扫描。</li>
 * </ul>
 */
@MapperScan("com.mvp.goods.mapper")
@SpringBootApplication(scanBasePackages = {"com.mvp.goods", "com.mvp.common"})
public class GoodsApplication {

    /**
     * 服务入口。
     *
     * <p>启动后会向 Nacos 注册自己（服务名 service-goods-0），并对外提供商品 CRUD
     * 与库存预扣/回补接口，其中库存接口主要供 service-order-0 通过 Feign 调用。</p>
     */
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(GoodsApplication.class, args);
    }
}
