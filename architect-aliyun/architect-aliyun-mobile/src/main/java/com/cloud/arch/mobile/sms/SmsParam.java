package com.cloud.arch.mobile.sms;

import lombok.Data;

@Data
public class SmsParam {

    /**
     * 短信签名
     */
    private String signName;
    /**
     * 短信模板code
     */
    private String template;
    /**
     * 发送短信手机号
     */
    private String phone;
    /**
     * 短信验证码
     */
    private String code;
    /**
     * 短信相关业务标识
     */
    private String bizId;
    /**
     * 验证码模板参数名称
     */
    private String placeHolder;
}
