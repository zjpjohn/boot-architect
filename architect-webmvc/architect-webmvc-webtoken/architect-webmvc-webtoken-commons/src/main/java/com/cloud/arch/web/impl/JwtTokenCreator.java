package com.cloud.arch.web.impl;

import com.cloud.arch.web.*;
import com.cloud.arch.web.utils.Assert;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
public record JwtTokenCreator(WebTokenProperties props) implements ITokenCreator {

    @Override
    public TokenResult create(IHttpAuthSource source, Map<String, Object> claims) {
        Assert.notNull(source, TokenErrorHandler.AUTH_SOURCE_NONNULL);
        Map<String, Object> allClaims = Maps.newHashMap(claims);
        //来源渠道
        allClaims.put(WebTokenConstants.JWT_CLAIM_SOURCE_KEY, source.value());
        //本次token标识
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        allClaims.put(WebTokenConstants.JWT_CLAIM_TOKEN_KEY, tokenId);
        //token信息
        JwtToken tokenInfo = new JwtToken(props.getExpire()
                                               .toMillis(), props.getIssuer(), props.getSecret(), allClaims);
        //创建token
        Pair<String, Date> pair = JwtTokenUtils.create(tokenInfo);
        return new TokenResult(pair.getKey(), tokenId, pair.getValue());
    }

}
