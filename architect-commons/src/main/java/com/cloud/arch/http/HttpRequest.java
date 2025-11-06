package com.cloud.arch.http;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class HttpRequest {

    private final CloseableHttpClient client;

    private static final class InstanceHolder {
        static HttpRequest httpRequest = new HttpRequest();
    }

    public static HttpRequest instance() {
        return InstanceHolder.httpRequest;
    }

    private HttpRequest() {
        try {
            SSLContext sslContext = SSLContexts.custom()
                                               .loadTrustMaterial(null, (certificates, socket) -> true)
                                               .build();
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                                                                                     .register("http", PlainConnectionSocketFactory.INSTANCE)
                                                                                     .register("https", new SSLConnectionSocketFactory(sslContext))
                                                                                     .build();
            PoolingHttpClientConnectionManager poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            poolManager.setMaxTotal(20);
            poolManager.setDefaultMaxPerRoute(5);
            //超时设置
            RequestConfig defaultConfig = RequestConfig.custom()
                                                       .setResponseTimeout(Timeout.of(20, TimeUnit.SECONDS))
                                                       .setConnectionRequestTimeout(Timeout.of(15, TimeUnit.SECONDS))
                                                       .build();
            //创建客户端
            this.client = HttpClients.custom()
                                     .setConnectionManager(poolManager)
                                     .setDefaultRequestConfig(defaultConfig)
                                     .build();
        } catch (Exception e) {
            throw new RuntimeException("initialize http request client exception.", e);
        }
    }

    /**
     * 校验URL
     */
    public Triple<Boolean, Long, LocalDateTime> check(String url) {
        return RequestWrapper.wrapper(new HttpGet(url)).checkExe(this);
    }

    /**
     * post with form-data
     */
    public String postForm(String url, Map<String, String> params) {
        return this.postForm(url, null, params);
    }

    /**
     * post with form-data
     */
    public String postForm(String url, Map<String, String> headers, Map<String, String> params) {
        return this.postForm(url, headers, params, StandardCharsets.UTF_8);
    }

    /**
     * post with form-data
     */
    public String postForm(String url, Map<String, String> headers, Map<String, String> params, Charset encoding) {
        return RequestWrapper.wrapper(new HttpPost(url), encoding)
                             .header(headers)
                             .pairs(params, encoding)
                             .execute(this);
    }

    /**
     * post request with xml
     */
    public String postXml(String url, String xml) {
        return postXml(url, xml, null);
    }

    /**
     * post request with xml
     */
    public String postXml(String url, String xml, Map<String, String> headers) {
        return this.postXml(url, xml, headers, StandardCharsets.UTF_8);
    }

    /**
     * post request with xml
     */
    public String postXml(String url, String xml, Map<String, String> headers, Charset encoding) {
        return RequestWrapper.wrapper(new HttpPost(url), encoding)
                             .header(headers)
                             .entity(new StringEntity(xml, StandardCharsets.UTF_8))
                             .execute(this);
    }

    /**
     * post request with byte
     */
    public String postBytes(String url, byte[] data) {
        return this.postBytes(url, null, data);
    }

    /**
     * post request with byte
     */
    public String postBytes(String url, Map<String, String> headers, byte[] data) {
        return RequestWrapper.wrapper(new HttpPost(url))
                             .header(headers)
                             .entity(new ByteArrayEntity(data, null))
                             .execute(this);
    }

    /**
     * get request
     */
    public String get(String url) {
        return this.get(url, null);
    }

    /**
     * get request
     */
    public String get(String url, Map<String, String> headers) {
        return this.get(url, headers, StandardCharsets.UTF_8);
    }

    /**
     * get request
     */
    public String get(String url, Map<String, String> headers, Charset encoding) {
        return RequestWrapper.wrapper(new HttpGet(url), encoding).header(headers).execute(this);
    }

    /**
     * put request form-data
     */
    public String putForm(String url, Map<String, String> params) {
        return this.putForm(url, null, params, StandardCharsets.UTF_8);
    }

    /**
     * put request form-data
     */
    public String putForm(String url, Map<String, String> headers, Map<String, String> params) {
        return this.putForm(url, params, headers, StandardCharsets.UTF_8);
    }

    /**
     * put request form-data
     */
    public String putForm(String url, Map<String, String> headers, Map<String, String> params, Charset encoding) {
        return RequestWrapper.wrapper(new HttpPut(url), encoding).header(headers).pairs(params, encoding).execute(this);
    }

    /**
     * put request with json
     */
    public String putJson(String url, String json) {
        return this.putJson(url, json, null, StandardCharsets.UTF_8);
    }

    /**
     * put request with json
     */
    public String putJson(String url, String json, Map<String, String> headers) {
        return this.putJson(url, json, headers, StandardCharsets.UTF_8);
    }

    /**
     * put request with json
     */
    public String putJson(String url, String json, Map<String, String> headers, Charset encoding) {
        Map<String, String> jsonHeader = Maps.newHashMap();
        jsonHeader.put("Content-type", "application/json;charset=utf-8");
        jsonHeader.put("Accept", "application/json");
        return RequestWrapper.wrapper(new HttpPut(url), encoding)
                             .header(headers)
                             .header(jsonHeader)
                             .entity(new StringEntity(json, encoding))
                             .execute(this);
    }

    /**
     * post request with json
     */
    public String postJson(String url, String json) {
        return this.postJson(url, json, null, StandardCharsets.UTF_8);
    }

    /**
     * post request with json
     */
    public String postJson(String url, String json, Map<String, String> headers) {
        return this.postJson(url, json, headers, StandardCharsets.UTF_8);
    }

    /**
     * post request with json
     */
    public String postJson(String url, String json, Map<String, String> headers, Charset encoding) {
        Map<String, String> jsonHeader = Maps.newHashMap();
        jsonHeader.put("Content-type", "application/json;charset=utf-8");
        jsonHeader.put("Accept", "application/json");
        return RequestWrapper.wrapper(new HttpPost(url), encoding)
                             .header(headers)
                             .header(jsonHeader)
                             .entity(new StringEntity(json, encoding))
                             .execute(this);
    }

    /**
     * get request download file
     */
    public <T> T download(String url, Function<InputStream, T> function) {
        return this.download(url, null, function);
    }

    /**
     * get request download file
     */
    public <T> T download(String url, Map<String, String> headers, Function<InputStream, T> function) {
        return RequestWrapper.wrapper(new HttpGet(url)).header(headers).execute(this, function);
    }

    /**
     * 请求返回流处理
     *
     * @param request  http请求
     * @param function 返回流处理
     */
    protected <T extends HttpUriRequest, V> V stream(T request, Function<InputStream, V> function) {
        return this.execute(request, entity -> {
            try {
                return function.apply(entity.getContent());
            } catch (IOException error) {
                throw new HttpRequestException(HttpRequestException.DEFAULT_ERROR, error);
            }
        });
    }

    /**
     * 验证url
     *
     * @param request get请求
     */
    protected <T extends HttpUriRequestBase> Triple<Boolean, Long, LocalDateTime> checkExe(T request) {
        LocalDateTime start = LocalDateTime.now();
        try {
            return client.execute(request, response -> {
                int  code  = response.getCode();
                long times = Duration.between(start, LocalDateTime.now()).toMillis();
                return Triple.of(code == HttpStatus.SC_OK, times, start);
            });
        } catch (IOException error) {
            return Triple.of(false, 0L, start);
        }
    }

    protected <T extends HttpUriRequest> String execute(T request, Charset encode) {
        return this.execute(request, entity -> {
            try {
                return EntityUtils.toString(entity, encode);
            } catch (IOException | ParseException error) {
                throw new HttpRequestException(error);
            }
        });
    }

    private <V, T extends HttpUriRequest> V execute(T request, Function<HttpEntity, V> handler) {
        try {
            HttpHost proxy = HttpProxyContext.getProxy();
            return client.execute(proxy, request, response -> {
                int        code   = response.getCode();
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new HttpRequestException(0, "http response entity must not be null.");
                }
                if (code != 200 && code != 301 && code != 302) {
                    String message = EntityUtils.toString(entity);
                    throw new HttpRequestException(0, String.format("http response status:[%d], error:%s.", code, message));
                }
                V result = handler.apply(entity);
                EntityUtils.consume(entity);
                return result;
            });
        } catch (IOException error) {
            throw new HttpRequestException(error);
        } finally {
            HttpProxyContext.clear();
        }
    }

}
