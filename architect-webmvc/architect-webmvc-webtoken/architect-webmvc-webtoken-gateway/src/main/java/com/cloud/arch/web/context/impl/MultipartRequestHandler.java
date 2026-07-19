package com.cloud.arch.web.context.impl;

import com.cloud.arch.web.context.AbsRequestHandler;
import com.cloud.arch.web.context.RequestContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.Map;

public class MultipartRequestHandler extends AbsRequestHandler {

    public MultipartRequestHandler(RequestContext context) {
        super(context);
    }

    @Override
    public Mono<ServerHttpRequest> handle(Map<String, Object> params, Map<String, String> headers) throws Exception {
        QueryRequestHandler handler = new QueryRequestHandler(this.context);
        return handler.handle(params, headers);
    }

}
