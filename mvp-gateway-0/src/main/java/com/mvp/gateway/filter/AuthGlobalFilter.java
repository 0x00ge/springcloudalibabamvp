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
 * Gateway accessToken 鉴权过滤器。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    public AuthGlobalFilter(JwtUtil jwtUtil,
                            ReactiveStringRedisTemplate redisTemplate,
                            AuthProperties authProperties) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    /**
     * 校验 accessToken。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = extractBearerToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "请先登录");
        }

        JwtPayload payload;
        try {
            payload = jwtUtil.parseAndValidate(token, JwtUtil.TYPE_ACCESS);
        } catch (IllegalArgumentException e) {
            return unauthorized(exchange, e.getMessage());
        }

        String blacklistKey = authProperties.getBlacklistKeyPrefix() + payload.getJti();
        return redisTemplate.hasKey(blacklistKey)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return unauthorized(exchange, "token 已登出");
                    }
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User-Id", payload.getSub())
                            .header("X-Token-Jti", payload.getJti())
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    /**
     * 鉴权应尽早执行，访问日志过滤器仍可在请求结束后记录最终路由。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isWhitelisted(String path) {
        return authProperties.getWhitelist().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.debug("Gateway 鉴权失败 path={} message={}", exchange.getRequest().getURI().getPath(), message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = JSON.toJSONBytes(errorBody(message));
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", message);
        body.put("data", null);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
