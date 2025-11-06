package com.cloud.arch.web;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.Map;

/**
 * @param expire token过期时间
 * @param issuer token发布者
 * @param salt   token加盐
 * @param claims token数据集合
 */
public record JwtToken(Long expire, String issuer, String salt, Map<String, Object> claims) {

    public JwtToken(Long expire, String issuer, String salt) {
        this(expire, issuer, salt, Maps.newHashMap());
    }

    public boolean validate() {
        return expire > 0L && !claims.isEmpty() && StringUtils.isNotBlank(issuer) && StringUtils.isNotBlank(salt);
    }

    public JwtToken claim(String key, Object value) {
        claims.put(key, value);
        return this;
    }

    public JwtToken claimAll(Map<String, Object> claims) {
        this.claims.putAll(claims);
        return this;
    }

    /**
     * 生成token
     *
     * @return token-token过期时间
     */
    public Pair<String, Date> generate() {
        return JwtTokenUtils.create(this);
    }

}
