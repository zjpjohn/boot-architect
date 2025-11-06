package com.cloud.arch.web.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Maps;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class ServletRequestParamWrapper extends HttpServletRequestWrapper {

    private static final char JSON_OBJECT_SYMBOL = '{';

    private final Map<String, String[]> parameterMap;
    private final Map<String, Object>   appendParameters;
    private final Map<String, String>   appendHeaders;

    public ServletRequestParamWrapper(HttpServletRequest request) {
        super(request);
        this.appendParameters = Maps.newHashMap();
        this.appendHeaders    = Maps.newHashMap();
        this.parameterMap     = Maps.newHashMap(request.getParameterMap());
    }

    public void putParameter(String name, Object value) {
        parameterMap.put(name, new String[] {value.toString()});
        appendParameters.put(name, value);
    }

    public void putParameters(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        params.forEach(this::putParameter);
    }

    public void retainParameter(String name) {
        parameterMap.remove(name);
    }

    public void retainParameters(List<String> names) {
        if (CollectionUtils.isEmpty(names)) {
            return;
        }
        names.forEach(parameterMap::remove);
    }

    public void putHeader(String key, String value) {
        appendHeaders.put(key, value);
    }

    public void putHeaders(Map<String, String> headers) {
        if (CollectionUtils.isEmpty(headers)) {
            return;
        }
        appendHeaders.putAll(headers);
    }

    @Override
    public String getHeader(String name) {
        if (appendHeaders.containsKey(name)) {
            return appendHeaders.get(name);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (appendHeaders.containsKey(name)) {
            List<String> headers = Collections.singletonList(appendHeaders.get(name));
            return Collections.enumeration(headers);
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headers = Collections.list(super.getHeaderNames());
        headers.addAll(appendHeaders.keySet());
        return Collections.enumeration(headers);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        //没有追加参数不重写输入流
        if (CollectionUtils.isEmpty(appendParameters)) {
            return super.getInputStream();
        }
        byte[] requestBody = StreamUtils.copyToByteArray(getRequest().getInputStream());
        if (requestBody.length > 0) {
            char jsonStart = (char)requestBody[0];
            if (JSON_OBJECT_SYMBOL == jsonStart) {
                //body体json数据,追加参数
                JSONObject jsonObject = JSON.parseObject(requestBody);
                jsonObject.putAll(appendParameters);
                //转换成二进制字符串
                requestBody = JSON.toJSONBytes(jsonObject);
            }
        }
        return new BodyInputStream(requestBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        if (values == null || values.length <= 0) {
            return null;
        }
        return values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return this.parameterMap.get(name);
    }

}
