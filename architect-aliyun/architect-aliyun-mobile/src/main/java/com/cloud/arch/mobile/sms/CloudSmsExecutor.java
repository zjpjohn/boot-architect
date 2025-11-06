package com.cloud.arch.mobile.sms;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class CloudSmsExecutor implements InitializingBean {

    private static final String TEMPLATE_PARAM      = """
            {
            "code":"%s"
            }
            """;
    private static final String DEFAULT_PLACEHOLDER = "code";
    private static final String DEFAULT_CHANNEL     = "thales";
    private static final String SUCCESS_FLAG        = "OK";

    private       Executor           executor;
    private       DefaultAcsClient   client;
    private final CloudSmsProperties properties;
    private final SmsFlowController  flowControl;

    public CloudSmsExecutor(CloudSmsProperties properties, SmsFlowController flowControl) {
        this.properties  = properties;
        this.flowControl = flowControl;
    }

    public CloudSmsExecutor(Executor executor, CloudSmsProperties properties, SmsFlowController flowControl) {
        this.executor    = executor;
        this.properties  = properties;
        this.flowControl = flowControl;
    }

    /**
     * 同步发送短信验证码
     *
     * @param param   短信验证码参数
     * @param channel 发送渠道
     * @param expire  过期时间
     */
    public SendResult syncSend(SmsParam param, String channel, Long expire) throws Exception {
        String smsChanel = StringUtils.isNotBlank(channel) ? channel : DEFAULT_CHANNEL;
        if (flowControl.flowLimit(param.getPhone(), smsChanel)) {
            return SendResult.limitError("验证码未过期");
        }
        SendSmsRequest  request  = request(param);
        SendSmsResponse response = this.client.getAcsResponse(request);
        if (!isSuccess(response)) {
            log.error("发送短信验证码[{}]失败，失败原因:{}", response.getCode(), response.getMessage());
            return SendResult.apiError("短信接口错误");
        }
        flowControl.cacheCode(param.getPhone(), smsChanel, param.getCode(), expire, TimeUnit.SECONDS);
        return SendResult.success("发送成功");
    }

    /**
     * 异步发送短信验证码
     *
     * @param param   短信参数
     * @param channel 发送渠道
     * @param expire  短信过期时间
     */
    public SendResult asyncSend(SmsParam param, String channel, Long expire) {
        if (executor == null) {
            throw new IllegalArgumentException("请配置异步发送线程池.");
        }
        String smsChanel = StringUtils.isNotBlank(channel) ? channel : DEFAULT_CHANNEL;
        if (flowControl.flowLimit(param.getPhone(), smsChanel)) {
            return SendResult.limitError("验证码未过期");
        }
        executor.execute(() -> {
            try {
                SendSmsRequest  request  = request(param);
                SendSmsResponse response = this.client.getAcsResponse(request);
                if (!isSuccess(response)) {
                    log.error("发送短信验证码[{}]失败，失败原因:{}", response.getCode(), response.getMessage());
                    return;
                }
                flowControl.cacheCode(param.getPhone(), smsChanel, param.getCode(), expire, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("发送短信验证码异常:", e);
            }
        });
        return SendResult.success("发送成功");
    }

    private Boolean isSuccess(SendSmsResponse response) {
        return SUCCESS_FLAG.equals(response.getCode());
    }

    /**
     * 构造发送请求
     *
     * @param param 短信参数
     */
    private SendSmsRequest request(SmsParam param) {
        SendSmsRequest request = new SendSmsRequest();
        request.setSignName(param.getSignName());
        request.setPhoneNumbers(param.getPhone());
        request.setTemplateCode(param.getTemplate());
        request.setTemplateParam(templateParam(param));
        if (StringUtils.isNotBlank(param.getBizId())) {
            request.setOutId(param.getBizId());
        }
        return request;
    }

    /**
     * 模板验证码参数替换
     *
     * @param param 短信参数
     */
    private String templateParam(SmsParam param) {
        String placeHolder = param.getPlaceHolder();
        if (StringUtils.isBlank(placeHolder)) {
            placeHolder = DEFAULT_PLACEHOLDER;
        }
        if (DEFAULT_PLACEHOLDER.equals(placeHolder)) {
            return String.format(TEMPLATE_PARAM, param.getCode());
        }
        String paramTemplate = TEMPLATE_PARAM.replace(DEFAULT_PLACEHOLDER, placeHolder);
        return String.format(paramTemplate, param.getCode());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        DefaultProfile profile = DefaultProfile.getProfile(properties.getRegion(),
                                                           properties.getAccessId(),
                                                           properties.getSecret());
        DefaultProfile.addEndpoint(properties.getRegion(), properties.getProduct(), properties.getEndpoint());
        this.client = new DefaultAcsClient(profile);
    }

}
