package com.boot.architect.application.command.impl;

import com.boot.architect.application.command.IAuthCommandService;
import com.boot.architect.application.command.dto.AuthLoginCmd;
import com.boot.architect.application.command.vo.AuthLoginResult;
import com.boot.architect.infrast.error.ResponseErrorHandler;
import com.cloud.arch.web.IHttpAuthSource;
import com.cloud.arch.web.IHttpAuthSourceManager;
import com.cloud.arch.web.ITokenCreator;
import com.cloud.arch.web.TokenResult;
import com.cloud.arch.web.utils.Assert;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandService implements IAuthCommandService {

    public static final String                 PASSWORD = "123456";
    private final       ITokenCreator          tokenCreator;
    private final       IHttpAuthSourceManager authSourceManager;

    @Override
    public AuthLoginResult userLogin(AuthLoginCmd command) {
        String          name       = command.getName();
        IHttpAuthSource authSource = authSourceManager.ofKey("user");
        if (!name.equals("zhangsan") || !PASSWORD.equals(command.getPassword())) {
            Assert.throwError(ResponseErrorHandler.AUTH_LOGIN_ERROR);
        }
        Map<String, Object> payload = Maps.newHashMap();
        payload.put(authSource.authKey(), "123456789");
        payload.put("name", command.getName());
        TokenResult     tokenResult = tokenCreator.create(authSource, payload);
        AuthLoginResult result      = new AuthLoginResult();
        result.setToken(tokenResult.token());
        result.setExpireAt(tokenResult.expireAt());
        return result;
    }

    @Override
    public AuthLoginResult managerLogin(AuthLoginCmd command) {
        String          name       = command.getName();
        IHttpAuthSource authSource = authSourceManager.ofKey("manager");
        if (!name.equals("lisi") || !PASSWORD.equals(command.getPassword())) {
            Assert.throwError(ResponseErrorHandler.AUTH_LOGIN_ERROR);
        }
        Map<String, Object> payload = Maps.newHashMap();
        payload.put(authSource.authKey(), "123456789");
        payload.put("name", command.getName());
        TokenResult     tokenResult = tokenCreator.create(authSource, payload);
        AuthLoginResult result      = new AuthLoginResult();
        result.setToken(tokenResult.token());
        result.setExpireAt(tokenResult.expireAt());
        return result;
    }

}
