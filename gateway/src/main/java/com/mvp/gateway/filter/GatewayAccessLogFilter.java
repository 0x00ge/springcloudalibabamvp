package com.mvp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Gateway 访问日志过滤器。
 *
 * <p>用于确认请求是否经过 gateway、命中了哪条路由、转发到了哪个下游服务。
 * 访问 {@code http://127.0.0.1:8000/user/page} 时，可以在 gateway 控制台或
 * {@code logs/mvp-gateway/app.log} 中看到本过滤器输出的日志。</p>
 */
@Slf4j
@Component
public class GatewayAccessLogFilter implements GlobalFilter, Ordered {

    private static final String UNKNOWN = "-";

    /**
     * 记录 gateway 请求链路。
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        // 响应提交前写入调试响应头，curl -i 可以直接看出这次请求是否经过 gateway。
        exchange.getResponse().beforeCommit(() -> {
            GatewayRouteInfo routeInfo = getRouteInfo(exchange);
            exchange.getResponse().getHeaders().add("X-Gateway-Route", routeInfo.routeId());
            exchange.getResponse().getHeaders().add("X-Gateway-Service", routeInfo.serviceName());
            return Mono.empty();
        });

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // 请求结束后，Gateway 已经完成路由和负载均衡，可以拿到最终转发地址。
                    long costMs = System.currentTimeMillis() - startTime;
                    GatewayRouteInfo routeInfo = getRouteInfo(exchange);
                    log.info("gateway access method={} path={} status={} routeId={} service={} target={} costMs={}",
                            getMethod(exchange),
                            getPath(exchange),
                            getStatus(exchange),
                            routeInfo.routeId(),
                            routeInfo.serviceName(),
                            routeInfo.targetUri(),
                            costMs);
                });
    }

    /**
     * 让本过滤器最先进入请求链，最后输出日志。
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private GatewayRouteInfo getRouteInfo(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        if (route == null) {
            return new GatewayRouteInfo(UNKNOWN, UNKNOWN, toText(targetUri));
        }

        URI routeUri = route.getUri();
        String serviceName = "lb".equalsIgnoreCase(routeUri.getScheme()) ? routeUri.getHost() : toText(routeUri);
        return new GatewayRouteInfo(route.getId(), serviceName, toText(targetUri));
    }

    private String getMethod(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod().name();
    }

    private String getPath(ServerWebExchange exchange) {
        URI uri = exchange.getRequest().getURI();
        if (uri.getRawQuery() == null) {
            return uri.getRawPath();
        }
        return uri.getRawPath() + "?" + uri.getRawQuery();
    }

    private String getStatus(ServerWebExchange exchange) {
        HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
        if (statusCode == null) {
            return UNKNOWN;
        }
        return String.valueOf(statusCode.value());
    }

    private String toText(URI uri) {
        return uri == null ? UNKNOWN : uri.toString();
    }

    private record GatewayRouteInfo(String routeId, String serviceName, String targetUri) {
    }
}
