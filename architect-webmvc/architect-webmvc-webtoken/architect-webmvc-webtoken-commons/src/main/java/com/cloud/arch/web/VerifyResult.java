package com.cloud.arch.web;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class VerifyResult {

    /**
     * jwt校验解析后追加参数
     */
    private final Map<String, Object> parameters = Maps.newHashMap();
    /**
     * jwt校验解析后追加header
     */
    private final Map<String, String> headers    = Maps.newHashMap();
    /**
     * jwt校验后形成的参数字段保留字段
     */
    private final List<String>        retains    = Lists.newArrayList();
    /**
     * 当前token的访问域
     */
    private final String              domain;

    public VerifyResult(String domain) {
        this.domain = domain;
    }

    public void parameter(String name, Object value) {
        this.parameters.put(name, value);
    }

    public void header(String name, String value) {
        this.headers.put(name, value);
    }

    public void addRetain(String retain) {
        this.retains.add(retain);
    }

    public void addRetainAll(Collection<String> retains) {
        this.retains.addAll(retains);
    }

}
