package com.cloud.arch.hotkey.config.props;

import lombok.Data;

@Data
public class HotKeyProperties {

    /**
     * 过期时间阈值
     */
    private Integer expireThreshold = 5000;
    /**
     * 线程数量
     */
    private Integer threadSize      = 1;
    /**
     * 缓存超时时间(单位秒)
     */
    private Integer cacheTimeout    = 60;
    /**
     * 是否开启监控
     */
    private boolean monitor         = true;

}
