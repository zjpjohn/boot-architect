package com.cloud.arch.web.impl;

import com.cloud.arch.web.*;
import com.cloud.arch.web.utils.Assert;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class JwtTokenVerifier implements ITokenVerifier {

    private final WebTokenProperties       props;
    private final ITokenBlackListValidator tokenValidator;
    private final IHttpAuthSourceManager   sourceManager;

    public JwtTokenVerifier(WebTokenProperties props,
                            ITokenBlackListValidator tokenValidator,
                            IHttpAuthSourceManager sourceManager) {
        this.props          = props;
        this.tokenValidator = tokenValidator;
        this.sourceManager  = sourceManager;
    }

    @Override
    public String header() {
        return props.getHeader();
    }

    @Override
    public VerifyResult verify(String token) {
        //token校验，成功返回payload数据
        Map<String, Object> payload = JwtTokenUtils.verify(token, props.getSecret());
        //token有效性校验
        String tokenId = (String) payload.get(WebTokenConstants.JWT_CLAIM_TOKEN_KEY);
        //token黑名单校验
        Assert.state(!tokenValidator.validate(tokenId), TokenErrorHandler.INVALID_AUTH_TOKEN);
        //授权渠道校验
        String source = (String) payload.get(WebTokenConstants.JWT_CLAIM_SOURCE_KEY);
        Assert.notNull(source, TokenErrorHandler.AUTH_SOURCE_ERROR);
        //请求域判断
        IHttpAuthSource authSource = Assert.notNull(sourceManager.ofKey(source), TokenErrorHandler.AUTH_SOURCE_UNKNOWN);
        //解析业务字段
        VerifyResult result = authSource.parse(payload);
        //用户身份标识
        Object identity = result.getParameters().get(authSource.authKey());
        //判断用户登录身份标识
        Assert.notNull(identity, TokenErrorHandler.AUTH_IDENTITY_UNKNOWN);
        //设置授权渠道标识header，为后续权限校验预留
        result.header(WebTokenConstants.ACCESS_SOURCE_HEADER, source);
        //设置访问者身份标识header，为后续权限校验预留
        result.header(WebTokenConstants.AUTH_IDENTITY_HEADER, identity.toString());
        return result;
    }

}
