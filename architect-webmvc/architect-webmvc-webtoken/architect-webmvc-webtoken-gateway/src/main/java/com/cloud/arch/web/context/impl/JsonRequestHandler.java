package com.cloud.arch.web.context.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cloud.arch.web.context.AbsRequestHandler;
import com.cloud.arch.web.context.RequestContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static com.cloud.arch.web.context.RequestContext.JSON_OBJECT_SYMBOL;

public class JsonRequestHandler extends AbsRequestHandler {

    public JsonRequestHandler(RequestContext context) {
        super(context);
    }

    @Override
    public Mono<ServerHttpRequest> handle(Map<String, Object> params, Map<String, String> headers) throws Exception {
        ServerWebExchange             exchange    = this.context.getExchange();
        ServerHttpRequest             request     = exchange.getRequest();
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        //Json Body请求uri上有参数，直接将参数追加在uri上,建议直接使用uri参数
        if (!CollectionUtils.isEmpty(queryParams)) {
            QueryRequestHandler handler = new QueryRequestHandler(this.context);
            return handler.handle(params, headers);
        }
        //请求uri上没有参数，需要处理Json参数
        return DataBufferUtils.join(request.getBody()).filter(Objects::nonNull).flatMap(dataBuffer -> {
            final byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            final char jsonStartIndicator = (char) bytes[0];
            byte[]     modifyBytes        = bytes;
            //判断请求body体为json对象且url上没有参数，将追加参数加到json对象上
            if (isJsonObject(jsonStartIndicator)) {
                JSONObject jsonObject = JSON.parseObject(bytes);
                jsonObject.putAll(params);
                modifyBytes = JSON.toJSONBytes(jsonObject);
            }
            //重写追加参数后的网关请求
            final byte[] jsonBytes = modifyBytes;
            ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {

                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.putAll(context.getHeaders());
                    if (!CollectionUtils.isEmpty(headers)) {
                        headers.forEach(httpHeaders::set);
                    }
                    httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                    long contentLength = jsonBytes.length;
                    if (contentLength > 0) {
                        httpHeaders.setContentLength(contentLength);
                    } else {
                        httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                    }
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    return httpHeaders;
                }

                @Override
                public URI getURI() {
                    //注意不要重写getQueryParams(),重写此方法不起作用,原因此方法仅返回解析好的只读型参数，不能修改
                    if (isJsonObject(jsonStartIndicator)) {
                        return super.getURI();
                    }
                    //Json数组参数直接追加在uri参数上
                    MultiValueMap<String, String> allParams = new LinkedMultiValueMap<>();
                    params.forEach((k, v) -> allParams.add(k, v.toString()));
                    return UriComponentsBuilder.fromUri(super.getURI())
                                               .replaceQueryParams(allParams)
                                               .build(false)
                                               .toUri();
                }

                @Override
                public Flux<DataBuffer> getBody() {
                    return Flux.defer(() -> {
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(jsonBytes);
                        DataBufferUtils.retain(buffer);
                        return Mono.just(buffer);
                    });
                }
            };
            return Mono.just(decorator);
        });
    }

    private boolean isJsonObject(char identity) {
        return identity == JSON_OBJECT_SYMBOL;
    }


}
