package com.cloud.arch.hotkey.config.props;

import lombok.Data;

@Data
public class WorkerServerProperties {

    /**
     * work服务器网络配置
     */
    private WorkerNetProperties  network;
    /**
     * etcd连接配置信息
     */
    private EtcdServerProperties etcd;
    /**
     * hotkey配置信息
     */
    private HotKeyProperties     hotKey;

}
