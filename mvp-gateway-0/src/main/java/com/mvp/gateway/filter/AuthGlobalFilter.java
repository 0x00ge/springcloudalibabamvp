package com.mvp.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.mvp.common.jwt.JwtPayload;
import com.mvp.common.jwt.JwtUtil;
import com.mvp.gateway.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gateway全局鉴权过滤器
 *
 * <p>功能说明：
 * <ul>
 *   <li>拦截所有经过Gateway的请求，进行统一的身份认证和权限校验</li>
 *   <li>支持白名单配置，对特定路径跳过鉴权（如登录、注册、健康检查等公开接口）</li>
 *   <li>从HTTP请求头的Authorization字段提取Bearer Token并进行JWT解析验证</li>
 *   <li>通过Redis黑名单机制校验Token是否已被强制登出</li>
 *   <li>鉴权通过后将用户信息（用户ID、Token唯一标识等）透传到下游微服务</li>
 *   <li>鉴权失败时返回统一的401未授权响应格式</li>
 * </ul>
 *
 * <p>执行时机：设置为最高优先级（HIGHEST_PRECEDENCE + 10），确保在业务路由之前执行
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /**
     * Bearer Token的固定前缀，符合RFC 6750标准
     * 请求头格式：Authorization: Bearer [token]
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Ant风格路径匹配器
     * 支持通配符匹配：
     *   ? 匹配单个字符
     *   * 匹配0个或多个字符（不包含路径分隔符）
     *   ** 匹配0个或多个路径段
     * 示例：/api/auth/** 匹配 /api/auth/refresh 等
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** JWT工具类，负责Token的生成、解析和验证 */
    private final JwtUtil jwtUtil;

    /** 响应式Redis模板，用于异步非阻塞地操作Redis（黑名单校验） */
    private final ReactiveStringRedisTemplate redisTemplate;

    /** 认证配置属性，包含白名单路径、黑名单Key前缀等配置项 */
    private final AuthProperties authProperties;

    /**
     * 构造方法注入依赖
     *
     * @param jwtUtil JWT工具类
     * @param redisTemplate 响应式Redis模板
     * @param authProperties 认证配置属性
     */
    public AuthGlobalFilter(JwtUtil jwtUtil,
                            ReactiveStringRedisTemplate redisTemplate,
                            AuthProperties authProperties) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    /**
     * 全局过滤器的核心处理方法，执行完整的请求鉴权流程
     *
     * <p>执行步骤：
     * <ol>
     *   <li>获取请求路径，判断是否在白名单中，如果是则直接放行</li>
     *   <li>从Authorization请求头中提取Bearer Token</li>
     *   <li>调用JwtUtil解析并验证Token（签名、有效期、Token类型等）</li>
     *   <li>从Token载荷中获取JTI（JWT唯一标识），构造Redis黑名单Key</li>
     *   <li>异步查询Redis，判断该Token是否已被加入黑名单（用户登出时添加）</li>
     *   <li>如果在黑名单中，返回"token已登出"错误；否则鉴权通过</li>
     *   <li>鉴权通过后，将用户ID和JTI添加到请求头中，传递给下游服务</li>
     *   <li>继续执行过滤器链</li>
     * </ol>
     *
     * @param exchange 服务器Web交换对象，封装了请求和响应的上下文信息
     * @param chain 过滤器链，用于将请求传递给下一个过滤器或目标服务
     * @return Mono<Void> 响应式编程的返回结果，表示异步处理完成
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取当前请求的路径（不包含查询参数）
        String path = exchange.getRequest().getURI().getPath();

        // 检查当前请求路径是否在白名单中，白名单路径跳过所有鉴权逻辑
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 从Authorization请求头中提取Bearer Token
        String token = extractBearerToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        // 如果Token为空或格式不正确，返回401未授权
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "请先登录");
        }

        JwtPayload payload;
        try {
            // 解析并验证JWT Token
            // 验证内容包括：签名是否正确、是否过期、Token类型是否为ACCESS类型
            payload = jwtUtil.parseAndValidate(token, JwtUtil.TYPE_ACCESS);
        } catch (IllegalArgumentException e) {
            // 捕获验证异常（签名错误、Token过期、类型不匹配等），返回具体错误信息
            return unauthorized(exchange, e.getMessage());
        }

        // 构造Redis黑名单Key，格式为：配置的前缀 + JTI
        // 例如：auth:blacklist:abc-123-def-456
        String blacklistKey = authProperties.getBlacklistKeyPrefix() + payload.getJti();

        // 使用响应式Redis异步检查Key是否存在
        // flatMap用于处理异步操作结果，并返回新的Mono
        return redisTemplate.hasKey(blacklistKey)
                .flatMap(exists -> {
                    // 如果Redis中存在该Key，说明Token已被加入黑名单（用户已登出或Token被撤销）
                    if (Boolean.TRUE.equals(exists)) {
                        return unauthorized(exchange, "token 已登出");
                    }

                    // 鉴权通过：构建新的请求对象，将用户信息添加到请求头中
                    // 下游微服务可以通过这些请求头获取当前用户信息
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User-Id", payload.getSub())      // 用户ID
                            .header("X-Token-Jti", payload.getJti())    // Token唯一标识
                            .build();

                    // 使用变更后的请求对象继续执行过滤器链
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    /**
     * 获取过滤器的执行顺序
     *
     * <p>返回 {@link Ordered#HIGHEST_PRECEDENCE} + 10，原因如下：
     * <ul>
     *   <li>HIGHEST_PRECEDENCE 表示最高优先级（数值最小）</li>
     *   <li>+10 让它在最高优先级中稍微靠后，为其他更核心的过滤器（如日志、监控）留出空间</li>
     *   <li>确保该过滤器在Spring Cloud Gateway的路由转发之前执行</li>
     * </ul>
     *
     * @return 优先级值，数值越小优先级越高
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /**
     * 判断请求路径是否在白名单中
     *
     * <p>使用AntPathMatcher进行路径匹配，支持Ant风格的通配符模式。
     * 白名单路径从配置文件中读取（auth.whitelist），常见配置示例：
     * <ul>
     *   <li>/api/auth/** - 匹配认证相关的所有接口（登录、注册、刷新Token等）</li>
     *   <li>/actuator/health - 精确匹配健康检查端点</li>
     *   <li>/api/public/* - 匹配 /api/public/info，但不匹配 /api/public/user/info</li>
     *   <li>/swagger-ui/** - 匹配Swagger文档相关路径</li>
     * </ul>
     *
     * @param path 当前请求的路径
     * @return true-在白名单中，跳过鉴权；false-不在白名单中，需要鉴权
     */
    private boolean isWhitelisted(String path) {
        // 遍历配置的白名单列表，只要有一个匹配就返回true
        return authProperties.getWhitelist().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * 从Authorization请求头中提取Bearer Token
     *
     * <p>标准Bearer Token格式要求：
     * <pre>
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * </pre>
     *
     * <p>提取逻辑：
     * <ol>
     *   <li>检查Authorization头是否存在且不为空</li>
     *   <li>检查是否以"Bearer "开头（注意有空格）</li>
     *   <li>如果满足条件，截取"Bearer "后面的Token字符串</li>
     * </ol>
     *
     * @param authorization Authorization请求头的值
     * @return 提取出的Token字符串，如果格式不正确则返回null
     */
    private String extractBearerToken(String authorization) {
        // 验证Authorization头是否存在，是否以"Bearer "开头
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        // 截取"Bearer "之后的Token部分
        return authorization.substring(BEARER_PREFIX.length());
    }

    /**
     * 返回401 Unauthorized响应，表示请求未授权
     *
     * <p>响应格式为统一的JSON结构：
     * <pre>
     * {
     *   "code": 401,
     *   "message": "错误信息",
     *   "data": null,
     *   "timestamp": 1700000000000
     * }
     * </pre>
     *
     * <p>响应头设置：
     * <ul>
     *   <li>HTTP状态码：401 Unauthorized</li>
     *   <li>Content-Type: application/json;charset=UTF-8</li>
     * </ul>
     *
     * @param exchange 服务器Web交换对象，用于获取响应对象
     * @param message 具体的错误提示信息
     * @return Mono<Void> 表示响应已写入完成
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        // 记录调试日志，包含请求路径和错误信息
        log.debug("Gateway 鉴权失败 path={} message={}", exchange.getRequest().getURI().getPath(), message);

        // 设置HTTP响应状态码为401未授权
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // 设置响应内容类型为JSON
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构建错误响应体，转换为JSON字节数组
        byte[] body = JSON.toJSONBytes(errorBody(message));
        // 将字节数组包装为Netty的DataBuffer
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);

        // 写入响应数据并完成响应
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 构建统一格式的错误响应体
     *
     * <p>使用LinkedHashMap保持字段插入顺序，确保响应的JSON字段顺序固定，
     * 便于前端统一解析和展示。
     *
     * @param message 错误提示信息
     * @return 包含错误信息的Map对象，将被序列化为JSON
     */
    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);                        // 业务状态码，与HTTP状态码保持一致
        body.put("message", message);                 // 用户友好的错误提示信息
        body.put("data", null);                       // 错误情况下数据字段为null
        body.put("timestamp", System.currentTimeMillis()); // 当前时间戳（毫秒）
        return body;
    }
}
