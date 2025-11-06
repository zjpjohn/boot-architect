package com.cloud.arch.mobile.verify;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.GetMobileRequest;
import com.aliyun.dypnsapi20170525.models.GetMobileResponse;
import com.aliyun.dypnsapi20170525.models.GetMobileResponseBody;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetMobileExecutor {

    public static final String SUCCESS = "OK";

    private final Client client;

    public GetMobileExecutor(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    /**
     * 根据移动端token换取手机号
     *
     * @param token 换取手机号token
     * @param outId 外部标识
     */
    public String getMobile(String token, String outId) {
        GetMobileRequest request = new GetMobileRequest();
        request.setAccessToken(token);
        request.setOutId(outId);
        GetMobileResponse result = null;
        try {
            result = client.getMobile(request);
        } catch (Exception e) {
            log.error("获取手机号请求错误:", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        GetMobileResponseBody response = result.getBody();
        if (!SUCCESS.equals(response.getCode())) {
            throw new RuntimeException("获取手机号失败");
        }
        return response.getGetMobileResultDTO().getMobile();
    }

}
