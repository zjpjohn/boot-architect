package com.cloud.arch.mobile.sms;

import java.util.concurrent.TimeUnit;

public interface SmsFlowController {

    /**
     * 缓存验证码
     *
     * @param phone    手机号
     * @param channel  发送渠道
     * @param code     验证码
     * @param expire   过期时间
     * @param timeUnit 时间单位
     */
    void cacheCode(String phone, String channel, String code, Long expire, TimeUnit timeUnit);

    /**
     * 校验短信验证码
     *
     * @param phone   手机号
     * @param channel 发送渠道
     * @param code    验证码
     */
    Boolean checkCode(String phone, String channel, String code);

    /**
     * 发送验证码流控校验
     *
     * @param phone   手机号
     * @param channel 发送渠道
     */
    Boolean flowLimit(String phone, String channel);

}
