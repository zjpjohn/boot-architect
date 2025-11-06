package com.cloud.arch.web.impl;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.IHttpAuthSource;
import com.cloud.arch.web.VerifyResult;
import com.cloud.arch.web.error.ApiBizException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 提供请求授权域默认实现
 * 仅授权标识作为保留字段和追加参数
 * 如需更多个性化字段解析请自行参照扩展
 */
@Getter
@AllArgsConstructor
public class DefaultHttpAuthSource implements IHttpAuthSource {

    private final String       authKey;
    private final String       domain;
    private final List<String> retain;

    public DefaultHttpAuthSource(String authKey, String domain) {
        this(authKey, domain, Lists.newArrayList());
    }

    @Override
    public String value() {
        return this.domain;
    }

    @Override
    public String authKey() {
        return this.authKey;
    }

    @Override
    public VerifyResult parse(Map<String, Object> payload) {
        VerifyResult result = new VerifyResult(this.domain);
        //保留参数字段，默认自动添加授权标识字段
        Set<String> retains = Sets.newHashSet(this.authKey);
        if (CollectionUtils.isNotEmpty(this.retain)) {
            retains.addAll(this.retain);
        }
        result.addRetainAll(retains);
        //授权标识字段作为参数追加到请求中
        Object authKeyValue = payload.get(this.authKey);
        if (authKeyValue == null) {
            throw new ApiBizException(HttpStatus.UNAUTHORIZED, 401, "授权标识为空.");
        }
        result.parameter(authKey, authKeyValue);
        return result;
    }

}
