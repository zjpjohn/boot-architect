package com.cloud.token.config;

import lombok.Data;

@Data
public class SignatureConfig {

    /**
     * api接口签名key
     */
    private String secretKey;
    /**
     * 接口调用允许时间差(单位-毫秒),-1-不校验时间差，默认5分钟
     */
    private long   timeDisparity = 1000 * 60 * 5L;

    public long getNonceExpire() {
        if (this.timeDisparity >= 0) {
            return timeDisparity / 1000;
        }
        return 60 * 60 * 24L;
    }

}
