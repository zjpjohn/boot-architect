package com.cloud.arch.web;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cloud.arch.web.error.ApiBizException;
import com.cloud.arch.web.utils.Assert;
import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@UtilityClass
public class JwtTokenUtils {

    /**
     * 生成token
     *
     * @param token token信息
     */
    public static Pair<String, Date> create(JwtToken token) {
        Assert.state(token != null && token.validate());
        Date   expire = new Date(System.currentTimeMillis() + token.expire());
        String json   = JSON.toJSONString(token.claims());
        String encode = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        JWTCreator.Builder builder = JWT.create()
                                        .withIssuer(token.issuer())
                                        .withExpiresAt(expire)
                                        .withClaim(WebTokenConstants.PAYLOAD_KEY, encode);
        Algorithm algorithm = Algorithm.HMAC256(token.salt());
        return Pair.of(builder.sign(algorithm), expire);
    }

    /**
     * 获取token中的载体数据
     *
     * @param token token信息
     */
    public static Map<String, Object> payload(String token) {
        Preconditions.checkState(StringUtils.isNotBlank(token));
        String encode  = JWT.decode(token).getClaim(WebTokenConstants.PAYLOAD_KEY).asString();
        String payload = new String(Base64.getDecoder().decode(encode), StandardCharsets.UTF_8);
        return JSON.parseObject(payload, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * token过期判断
     *
     * @param token token信息
     */
    public static boolean isExpired(String token) {
        try {
            Date expire = JWT.decode(token).getClaim("exp").asDate();
            return expire.compareTo(new Date()) <= 0;
        } catch (JWTDecodeException e) {
            log.error("token过期校验异常:", e);
        }
        return Boolean.FALSE;
    }

    /**
     * 验证token
     *
     * @param token token信息
     * @param salt  token盐
     */
    public static Map<String, Object> verify(String token, String salt) {
        DecodedJWT verify;
        try {
            verify = JWT.require(Algorithm.HMAC256(salt)).build().verify(token);
        } catch (SignatureVerificationException error) {
            throw new ApiBizException(HttpStatus.UNAUTHORIZED.value(), WebTokenConstants.TOKEN_SIGNATURE_ERROR);
        } catch (TokenExpiredException error) {
            throw new ApiBizException(HttpStatus.UNAUTHORIZED.value(), WebTokenConstants.TOKEN_EXPIRED_ERROR);
        } catch (InvalidClaimException error) {
            throw new ApiBizException(HttpStatus.UNAUTHORIZED.value(), WebTokenConstants.TOKEN_ILLEGAL_ERROR);
        } catch (JWTVerificationException error) {
            throw new ApiBizException(HttpStatus.UNAUTHORIZED.value(), WebTokenConstants.TOKEN_VERIFIED_ERROR);
        }
        String encode = verify.getClaim(WebTokenConstants.PAYLOAD_KEY).asString();
        if (StringUtils.isBlank(encode)) {
            throw new ApiBizException(HttpStatus.UNAUTHORIZED.value(), WebTokenConstants.TOKEN_INVALID_ERROR);
        }
        String payload = new String(Base64.getDecoder().decode(encode), StandardCharsets.UTF_8);
        return JSON.parseObject(payload, new TypeReference<>() {
        });
    }

}
