package com.cloud.arch.event.remoting;

import lombok.Data;

@Data
public class RemotingProperties {

    /**
     * 连接超时间时间
     */
    private int connectTimeout        = 20000;
    /**
     * 响应超时时间
     */
    private int readTimeout           = 15000;
    /**
     * 允许最大连接
     */
    private int maxConnections        = 512;
    /**
     * 每个主机允许最大连接
     */
    private int maxConnectionsPerHost = 64;

}
