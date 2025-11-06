package com.cloud.arch.client;

import lombok.Data;

@Data
public class ElasticSearchProperties {

    private String         index;
    private String         server;
    private Integer        connectTimeout           = 1500;
    private Integer        socketTimeout            = 3000;
    private Integer        connectionRequestTimeout = 1500;
    private Integer        maxConnTotal             = 20;
    private Long           keepAlive                = 60000L;
    private Integer        maxConnPerRoute          = 10;
    private Authentication auth;

    @Data
    public static class Authentication {

        private String user;
        private String password;

    }
}
