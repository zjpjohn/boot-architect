package com.cloud.token.config;

import lombok.Data;

@Data
public class TokenConfig {

    /**
     * token存储名称，会加入存储key中
     */
    private String    tokenName            = "tk";
    /**
     * token有效期（单位-秒） 默认7天；-1表示永久有效
     */
    private long      timeout              = 60 * 60 * 24 * 7L;
    /**
     * token最低活跃时间(单位-秒)，token超过此时间没有访问系统就冻结token，默认-1-不限制，永不冻结
     */
    private long      activeTimeout        = -1;
    /**
     * 是否动态调整activeTimeout功能，默认不开启动态调整
     */
    private boolean   dynamicActiveTimeout = false;
    /**
     * 是否允许同一账户多端登录：true-允许多端登录，false-新登录挤掉旧登录
     */
    private boolean   concurrent           = true;
    /**
     * 是否允许多人登录同一账户共享token：true-所有登录公用一个token，false-每次登陆新建token
     */
    private boolean   share                = true;
    /**
     * 同一账户最大登录设备数:-1-不限制(concurrent=true,share=false配置时生效)
     */
    private int       maxLoginCount        = 12;
    /**
     * 每次创建token时最大循环次数，用于保证token唯一性，-1-不循环重试，直接使用
     */
    private int       maxRetryTimes        = 6;
    /**
     * token生成风格（默认可取值：uuid、simple-uuid、random-32、random-64、random-128）
     */
    private String    tokenStyle           = "uuid";
    /**
     * 是否开启自动续签activeTimeout: true-自动续签token活跃时间
     */
    private boolean   autoRenew            = true;
    /**
     * 无需授权接口uri,多个用逗号分隔
     */
    private String    authExcludes         = "";
    /**
     * 系统错误码
     */
    private ErrorCode code                 = new ErrorCode();
    /**
     * token存储header中前缀
     */
    private String    headerPrefix         = "";
    /**
     * jwt密钥：jwt模块使用生成的token有效
     */
    private String    jwtSecretKey;

    @Data
    public static class ErrorCode {
        /**
         * 接口授权错误码
         */
        private int auth     = 10401;
        /**
         * 权限校验错误码
         */
        private int security = 10403;
        /**
         * 接口禁言错误码
         */
        private int muted    = 11403;
        /**
         * 二次校验错误码
         */
        private int dual     = 11401;
        /**
         * 系统内部错误码
         */
        private int error    = 10500;
    }

}
