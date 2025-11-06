package com.cloud.arch.http;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Data
@NoArgsConstructor
public class RequestWrapper {

    private HttpUriRequestBase request;
    private Charset            encoding;

    public RequestWrapper(HttpUriRequestBase request, Charset encoding) {
        this.request  = request;
        this.encoding = encoding;
        this.setHttpCredential();
    }

    /**
     * 设置请求basic auth credential
     */
    private void setHttpCredential() {
        String credential = HttpAuthCredentials.basicCredential();
        if (StringUtils.isNoneBlank(credential)) {
            this.request.setHeader(HttpHeaders.AUTHORIZATION, credential);
            HttpAuthCredentials.clear();
        }
    }

    public static RequestWrapper wrapper(HttpUriRequestBase request) {
        return wrapper(request, StandardCharsets.UTF_8);
    }

    public static RequestWrapper wrapper(HttpUriRequestBase request, Charset encoding) {
        return new RequestWrapper(request, encoding);
    }

    public RequestWrapper header(Map<String, String> header) {
        if (header != null && !header.isEmpty()) {
            header.forEach(request::addHeader);
        }
        return this;
    }

    public RequestWrapper entity(AbstractHttpEntity entity) {
        if (entity != null) {
            this.request.setEntity(entity);
        }
        return this;
    }

    public RequestWrapper pairs(Map<String, String> params, Charset encoding) {
        if ((params != null && !params.isEmpty())) {
            List<NameValuePair> valuePairs = new ArrayList<>();
            params.forEach((k, v) -> valuePairs.add(new BasicNameValuePair(k, v)));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairs, encoding);
            this.request.setEntity(entity);
        }
        return this;
    }

    /**
     * 请求校验
     */
    public Triple<Boolean, Long, LocalDateTime> checkExe(HttpRequest request) {
        return request.checkExe(this.request);
    }

    /**
     * 执行请求
     */
    public String execute(HttpRequest executor) {
        return executor.execute(this.request, this.encoding);
    }

    /**
     * 请求流
     */
    public <V> V execute(HttpRequest executor, Function<InputStream, V> function) {
        return executor.stream(this.request, function);
    }

}
