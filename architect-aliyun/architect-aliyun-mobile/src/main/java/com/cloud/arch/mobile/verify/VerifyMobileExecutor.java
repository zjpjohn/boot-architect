package com.cloud.arch.mobile.verify;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.VerifyMobileRequest;
import com.aliyun.dypnsapi20170525.models.VerifyMobileResponse;
import com.aliyun.dypnsapi20170525.models.VerifyMobileResponseBody;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerifyMobileExecutor {

    public static final String SUCCESS = "OK";

    private final Client client;

    public VerifyMobileExecutor(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    /**
     * 校验手机号好是否为本机号码
     *
     * @param token 移动端请求token
     * @param phone 待校验手机号
     * @param outId 外部流水号
     */
    public VerifyResult verify(String token, String phone, String outId) {
        VerifyMobileRequest request = new VerifyMobileRequest();
        request.setAccessCode(token);
        request.setPhoneNumber(phone);
        request.setOutId(outId);
        VerifyMobileResponse result = null;
        try {
            result = client.verifyMobile(request);
        } catch (Exception e) {
            log.error("校验手机号请求错误:", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        VerifyMobileResponseBody response = result.getBody();
        if (!SUCCESS.equals(response.getCode())) {
            throw new RuntimeException("校验手机号失败.");
        }
        return VerifyResult.of(response.getGateVerifyResultDTO().getVerifyResult());
    }

}
