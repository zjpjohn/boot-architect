package com.cloud.arch.event.remoting;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.event.RocksCompensateProperties;
import com.cloud.arch.event.reparation.ReparationConstants;
import com.cloud.arch.event.reparation.ReparationRequest;
import com.cloud.arch.event.reparation.ReparationResponse;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.*;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpRemoting {

    private final AsyncHttpClient         httpClient;
    private final Map<String, String>     headers;
    private final RemotingResponseHandler responseHandler;

    public HttpRemoting(RocksCompensateProperties properties, RemotingResponseHandler responseHandler) {
        //授权header
        Assert.state(StringUtils.isNotBlank(properties.getAppKey()), "appKey must not be null.");
        Assert.state(StringUtils.isNotBlank(properties.getAccessToken()), "accessToken must not be null.");
        headers = Maps.newHashMap();
        headers.put(ReparationConstants.REPARATION_KEY_HEADER, properties.getAppKey());
        headers.put(ReparationConstants.REPARATION_ACCESS_HEADER, properties.getAccessToken());
        //http请求配置
        final RemotingProperties remoting = properties.getRemoting();
        DefaultAsyncHttpClientConfig clientConfig
                = new DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(remoting.getConnectTimeout())
                                                            .setReadTimeout(remoting.getReadTimeout())
                                                            .setMaxConnections(remoting.getMaxConnections())
                                                            .setMaxConnectionsPerHost(remoting.getMaxConnectionsPerHost())
                                                            .build();
        this.httpClient      = new DefaultAsyncHttpClient(clientConfig);
        this.responseHandler = responseHandler;
    }

    /**
     * 存储失败事件，以便后续集中补偿发送
     *
     * @param url     补偿服务地址
     * @param request 补偿数据请求
     */
    public void post(String url, ReparationRequest request) {
        final BoundRequestBuilder requestBuilder = buildRequest(url, request);
        final Stopwatch           stopwatch      = Stopwatch.createStarted();
        try {
            final Response response = requestBuilder.execute().get();
            if (log.isInfoEnabled()) {
                log.info("请求补偿事件[{}]耗时[{}]ms,响应成功.", request.getEventId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
            final String             responseBody       = response.getResponseBody(StandardCharsets.UTF_8);
            final ReparationResponse reparationResponse = JSON.parseObject(responseBody, ReparationResponse.class);
            responseHandler.onHandle(reparationResponse);
        } catch (Exception error) {
            if (log.isErrorEnabled()) {
                log.error("补偿事件[{}]处理失败:", request.getEventId(), error);
            }
        }
    }

    /**
     * 构造请求
     *
     * @param url  请求地址
     * @param data 请求数据
     */
    private BoundRequestBuilder buildRequest(String url, Object data) {
        final String              body    = JSON.toJSONString(data);
        final BoundRequestBuilder builder = httpClient.preparePost(url).setBody(body);
        builder.setSingleHeaders(headers);
        builder.setHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        builder.setHeader(HttpHeaderNames.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
        return builder;
    }

}
