package com.cloud.arch.web.context;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@Slf4j
@ToString
public class RequestContext {

    public static final char JSON_OBJECT_SYMBOL = '{';

    /**
     * 请求exchange
     */
    private ServerWebExchange  exchange;
    /**
     * 请求方法
     */
    private HttpMethod         method;
    /**
     * 请求header信息
     */
    private HttpHeaders        headers;
    /**
     * 请求数据类型
     */
    private MediaType          mediaType;
    /**
     * 请求处理类枚举
     */
    private RequestEnum        type;
    /**
     * 参数保留字段
     */
    private List<String>       retains;
    /**
     * 请求处理chain
     */
    private GatewayFilterChain chain;

    public RequestContext(ServerWebExchange exchange, GatewayFilterChain chain, List<String> retains) {
        this.exchange = exchange;
        this.chain    = chain;
        this.retains  = retains;
        this.initialize();
    }


    /**
     * 初始化请求context
     */
    private void initialize() {
        ServerHttpRequest request = this.exchange.getRequest();
        this.method    = request.getMethod();
        this.headers   = request.getHeaders();
        this.mediaType = Optional.ofNullable(this.headers.getContentType())
                                 .orElse(MediaType.APPLICATION_FORM_URLENCODED);
        this.type      = RequestEnum.valueOf(this.method, this.mediaType);
    }

    /**
     * 请求参数和请求头处理
     *
     * @param params  请求参数
     * @param headers 请求头信息
     */
    public Mono<Void> handle(Map<String, Object> params, Map<String, String> headers) throws Exception {
        if (this.type == null) {
            return this.chain.filter(exchange);
        }
        return this.type.build(this).handle(params, headers).flatMap(this::filter);
    }


    private Mono<Void> filter(ServerHttpRequest request) {
        ServerWebExchange webExchange = this.exchange.mutate().request(request).build();
        return this.chain.filter(webExchange);
    }


}
