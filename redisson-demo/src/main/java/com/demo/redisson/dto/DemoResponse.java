package com.demo.redisson.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * demo 接口统一响应对象。
 *
 * <p>真实业务中通常会使用 common 模块里的 ResultVO 或统一响应模型；
 * 这里单独定义一个轻量 DTO，是为了让 redisson-demo 能独立启动、独立阅读。</p>
 *
 * @param <T> data 字段的实际类型，比如 Map<String, Object>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoResponse<T> {

    /**
     * 请求是否成功。
     *
     * <p>当前 demo 只封装成功响应；异常场景由 Spring Boot 默认异常处理返回。</p>
     */
    private boolean success;

    /**
     * 简短说明本次调用演示了什么。
     */
    private String message;

    /**
     * 具体演示结果。
     *
     * <p>大多数接口返回 Map，便于同时展示多个 Redisson API 的返回值。</p>
     */
    private T data;

    /**
     * 响应生成时间。
     *
     * <p>用于观察接口是否真的重新执行，而不是浏览器或客户端缓存结果。</p>
     */
    private LocalDateTime timestamp;

    /**
     * 创建成功响应的静态工厂方法。
     *
     * @param message 响应说明
     * @param data 响应数据
     * @param <T> data 类型
     * @return 成功响应
     */
    public static <T> DemoResponse<T> ok(String message, T data) {
        return new DemoResponse<>(true, message, data, LocalDateTime.now());
    }
}
