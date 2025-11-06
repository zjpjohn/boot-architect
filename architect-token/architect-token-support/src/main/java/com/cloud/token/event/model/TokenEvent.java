package com.cloud.token.event.model;

import lombok.Data;

@Data
public abstract class TokenEvent {

    /**
     * token值
     */
    private String token;
    /**
     * 登录授权标识
     */
    private Object loginId;
    /**
     * 业务领域类型
     */
    private String realm;

}
