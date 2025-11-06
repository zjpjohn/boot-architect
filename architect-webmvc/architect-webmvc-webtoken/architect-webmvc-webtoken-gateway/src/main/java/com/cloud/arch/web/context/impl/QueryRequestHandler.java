package com.cloud.arch.web.context.impl;

import com.cloud.arch.web.context.AbsRequestHandler;
import com.cloud.arch.web.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
public class QueryRequestHandler extends AbsRequestHandler {

    public QueryRequestHandler(RequestContext context) {
        super(context);
    }

    @Override
    public Mono<ServerHttpRequest> handle(Map<String, Object> params, Map<String, String> headers) throws Exception {
        ServerWebExchange             exchange  = this.context.getExchange();
        ServerHttpRequest             request   = exchange.getRequest();
        List<String>                  retains   = this.context.getRetains();
        MultiValueMap<String, String> allParams = new LinkedMultiValueMap<>();
        //复制原始参数
        request.getQueryParams()
               .entrySet()
               .stream()
               .filter(entry -> !retains.contains(entry.getKey()))
               .forEach(entry -> allParams.put(entry.getKey(), entry.getValue()));
        //添加新参数
        params.forEach((key, value) -> allParams.add(key, value.toString()));
        //追加替换请求参数
        URI uri = UriComponentsBuilder.fromUri(request.getURI()).replaceQueryParams(allParams).build(false).toUri();
        //构造新的请求
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate().uri(uri);
        if (!CollectionUtils.isEmpty(headers)) {
            builder.headers(header -> headers.forEach(header::set));
        }
        return Mono.just(builder.build());
    }

}
