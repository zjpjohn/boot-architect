package com.cloud.arch.web.context;

import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class AbsRequestHandler {

    protected final RequestContext context;

    public AbsRequestHandler(RequestContext context) {
        this.context = context;
    }

    /**
     * 请求重写
     *
     * @param params  追加的请求参数
     * @param headers 追加的请求头
     */
    public abstract Mono<ServerHttpRequest> handle(Map<String, Object> params, Map<String, String> headers)
            throws Exception;

}
