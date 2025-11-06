package com.cloud.arch.web;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.web.context.RequestContext;
import com.cloud.arch.web.domain.BodyData;
import com.cloud.arch.web.error.ApiBizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private final ITokenVerifier      tokenVerifier;
    private final IAuthRequestExclude requestExclude;

    public GatewayAuthFilter(ITokenVerifier tokenVerifier, IAuthRequestExclude requestExclude) {
        this.tokenVerifier  = tokenVerifier;
        this.requestExclude = requestExclude;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String            path    = request.getPath().toString();
        String            method  = request.getMethod().name();
        if (requestExclude.isExclude(path, method)) {
            return chain.filter(exchange);
        }
        //授权token校验
        HttpHeaders        headers  = request.getHeaders();
        String             token    = headers.getFirst(tokenVerifier.header());
        ServerHttpResponse response = exchange.getResponse();
        if (!StringUtils.hasText(token)) {
            return errorResponse(response, HttpStatus.UNAUTHORIZED, WebTokenConstants.UNAUTHORIZED_MESSAGE, HttpStatus.UNAUTHORIZED.value());
        }
        try {
            VerifyResult result = tokenVerifier.verify(token);
            Route        route  = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            //访问渠道校验
            if (!checkChannel(route, result.getDomain())) {
                return errorResponse(response, HttpStatus.FORBIDDEN, WebTokenConstants.FORBIDDEN_MESSAGE, HttpStatus.FORBIDDEN.value());
            }
            RequestContext context = new RequestContext(exchange, chain, result.getRetains());
            return context.handle(result.getParameters(), result.getHeaders());
        } catch (ApiBizException e) {
            return errorResponse(response, HttpStatus.UNAUTHORIZED, e.getError(), e.getCode());
        } catch (Exception e) {
            log.error("网关系统错误:", e);
            return errorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, WebTokenConstants.SERVER_EXCEPTION, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean checkChannel(Route route, String channel) {
        return Optional.ofNullable(route)
                       .map(Route::getMetadata)
                       .map(v -> (List<String>) v.get("channels"))
                       .map(v -> v.contains(channel))
                       .orElse(true);
    }

    /**
     * 返回错误响应信息
     *
     * @param response 响应信息
     * @param status   响应状态
     * @param error    错误信息
     * @param code     错误状态码
     */
    public Mono<Void> errorResponse(ServerHttpResponse response, HttpStatus status, String error, Integer code) {
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        BodyData<Serializable> data   = new BodyData<>(error, null, code, null);
        DataBuffer             buffer = response.bufferFactory().wrap(JSON.toJSONBytes(data));
        response.setStatusCode(status);
        return response.writeWith(Flux.just(buffer));
    }


    /**
     * 授权filter执行顺序
     * 注意：系统设定值-10，如需加入其他filter，请自行控制好filter顺序
     */
    @Override
    public int getOrder() {
        return WebTokenConstants.AUTH_FILTER_ORDER;
    }

}
