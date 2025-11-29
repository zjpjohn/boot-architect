package com.cloud.arch.mobile.sms;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Getter
public class CloudSmsExecutor implements InitializingBean {

    private static final String TEMPLATE_PARAM      = """
            {
            "code":"%s"
            }
            """;
    private static final String DEFAULT_PLACEHOLDER = "code";
    private static final String DEFAULT_CHANNEL     = "default";
    private static final String SUCCESS_FLAG        = "OK";

    private final ExecutorService    executor;
    private final CloudSmsProperties properties;
    private final SmsFlowController  flowControl;
    private       DefaultAcsClient   client;

    public CloudSmsExecutor(CloudSmsProperties properties, SmsFlowController flowControl) {
        this.properties = properties;
        this.flowControl = flowControl;
        ThreadFactory factory = Thread.ofVirtual().name("sms-send-thread-").factory();
        this.executor = Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * 同步发送短信验证码
     */
    public SendResult syncSend(SmsParam param, String channel, Long expire) throws Exception {
        String smsChanel = StringUtils.isNotBlank(channel) ? channel : DEFAULT_CHANNEL;
        if (flowControl.flowLimit(param.getPhone(), smsChanel)) {
            return SendResult.limitError("verify code not expired");
        }
        SendSmsRequest  request  = request(param);
        SendSmsResponse response = this.client.getAcsResponse(request);
        if (!isSuccess(response)) {
            log.error("发送短信验证码[{}]失败，失败原因:{}", response.getCode(), response.getMessage());
            return SendResult.apiError("sms response error");
        }
        flowControl.cacheCode(param.getPhone(), smsChanel, param.getCode(), expire, TimeUnit.SECONDS);
        return SendResult.success("send success");
    }

    /**
     * 异步发送短信验证码
     */
    public void asyncSend(SmsParam param, String channel, Long expire) {
        executor.execute(() -> {
            try {
                String smsChannel = this.smsChannel(channel);
                if (flowControl.flowLimit(param.getPhone(), smsChannel)) {
                    return;
                }
                SendSmsRequest  request  = request(param);
                SendSmsResponse response = this.client.getAcsResponse(request);
                if (!isSuccess(response)) {
                    log.error("发送短信验证码[{}]失败，失败原因:{}", response.getCode(), response.getMessage());
                    return;
                }
                flowControl.cacheCode(param.getPhone(), smsChannel, param.getCode(), expire, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("发送短信验证码异常:", e);
            }
        });
    }

    private Boolean isSuccess(SendSmsResponse response) {
        return SUCCESS_FLAG.equals(response.getCode());
    }


    private String smsChannel(String smsChanel) {
        return Optional.ofNullable(smsChanel).filter(StringUtils::isNotBlank).orElse(DEFAULT_CHANNEL);
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
