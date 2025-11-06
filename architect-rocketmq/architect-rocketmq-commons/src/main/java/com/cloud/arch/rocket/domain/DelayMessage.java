package com.cloud.arch.rocket.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DelayMessage<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -8841740105979051408L;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息过滤tag
     */
    private String tag;

    /**
     * 消息内容
     */
    private T body;

    /**
     * 消息延迟级别
     */
    private Set<Long> delivers;

    /**
     * 业务标识
     */
    private String bizKey;
}
