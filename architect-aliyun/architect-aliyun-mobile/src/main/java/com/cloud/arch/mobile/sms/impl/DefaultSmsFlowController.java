package com.cloud.arch.mobile.sms.impl;


import com.cloud.arch.mobile.sms.SmsFlowController;

import java.util.concurrent.TimeUnit;

public class DefaultSmsFlowController implements SmsFlowController {
    /**
     * 缓存验证码
     *
     * @param phone    手机号
     * @param channel  发送渠道
     * @param code     验证码
     * @param expire   过期时间
     * @param timeUnit 时间单位
     */
    @Override
    public void cacheCode(String phone, String channel, String code, Long expire, TimeUnit timeUnit) {

    }

    /**
     * 校验短信验证码
     *
     * @param phone   手机号
     * @param channel 发送渠道
     * @param code    验证码
     */
    @Override
    public Boolean checkCode(String phone, String channel, String code) {
        return true;
    }

    /**
     * 发送验证码流控校验
     *
     * @param phone   手机号
     * @param channel 发送渠道
     */
    @Override
    public Boolean flowLimit(String phone, String channel) {
        return true;
    }
}
