package com.cloud.token.token;

import lombok.Data;

@Data
public class AuthToken {

    /**
     * token 值
     */
    private String token;
    /**
     * token所属业务域类型
     */
    private String realm;
    /**
     * token登录后对应账户标识
     */
    private Object loginId;
    /**
     * token剩余存活时间
     */
    private Long   tokenTtl;
    /**
     * 当前会话剩余存活时间
     */
    private Long   sessionTtl;
    /**
     * 登录设备标识
     */
    private String device;
    /**
     * 自定义属性(作为扩展使用)
     */
    private String attr;

}
