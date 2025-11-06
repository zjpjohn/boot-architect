package com.cloud.arch.cache.props;

import lombok.Data;

@Data
public class HotKeyCacheProperties {

    /**
     * 热key探测应用名称
     */
    private String appName;
    /**
     * etcd连接地址，集群地址用逗号分割
     */
    private String etcdServer;
    /**
     * 热key统计上报时间间隔(毫秒)
     */
    private Long   hotReportInterval   = 500L;
    /**
     * 热key规则统计上报时间间隔(秒)
     */
    private Long   countReportInterval = 10L;

}
