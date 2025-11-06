package com.cloud.arch.hotkey.config.props;

import lombok.Data;

@Data
public class WorkerNetProperties {

    /**
     * netty服务端口
     */
    private Integer port;
    /**
     * 心跳间隔时间
     */
    private Integer heartInterval;
    /**
     * 超时时间
     */
    private Long    timeout;
    /**
     * 绑定ip地址，无法获取到ip时需要绑定
     */
    private String  bind;

}
