package com.cloud.arch.web.context.impl;

import com.cloud.arch.web.context.AbsRequestHandler;
import com.cloud.arch.web.context.RequestContext;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class FormRequestHandler extends AbsRequestHandler {

    public FormRequestHandler(RequestContext context) {
        super(context);
    }

    @Override
    public Mono<ServerHttpRequest> handle(Map<String, Object> params, Map<String, String> headers) throws Exception {
        ServerWebExchange             exchange = this.context.getExchange();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        return exchange.getFormData().doOnNext(formData::addAll).then(Mono.defer(() -> {
            //追加请求参数
            MultiValueMap<String, String> bodyData = new LinkedMultiValueMap<>();
            List<String>                  retains  = this.context.getRetains();
            //复制原始参数
            formData.entrySet()
                    .stream()
                    .filter(entry -> !retains.contains(entry.getKey()))
                    .forEach(entry -> bodyData.put(entry.getKey(), entry.getValue()));
            //加入新参数
            params.forEach((key, value) -> bodyData.add(key, value.toString()));
            //追加请求头,注：从请求中获取的请求为ReadOnlyHeaders,不允许set需新建一份
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.putAll(context.getHeaders());
            if (!CollectionUtils.isEmpty(headers)) {
                headers.forEach(httpHeaders::set);
            }
            httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            //重新构造请求信息
            CachedBodyOutputMessage message = new CachedBodyOutputMessage(exchange, httpHeaders);
            BodyInserter<MultiValueMap<String, String>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromValue(
                    bodyData);
            return bodyInserter.insert(message, new BodyInserterContext()).then(Mono.defer(() -> {
                ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        return httpHeaders;
                    }

                    @Override
                    public Flux<DataBuffer> getBody() {
                        return message.getBody();
                    }
                };
                return Mono.just(decorator);
            }));
        }));
    }
}
