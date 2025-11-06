package com.cloud.arch.event.props;


import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
public class RocketmqV5Properties {

    /**
     * 客户端连接授权key,云Rocketmq5.0使用
     */
    private String           accessKey      = "";
    /**
     * 客户端连接授权secret,云Rocketmq5.0使用
     */
    private String           secretKey      = "";
    /**
     * 客户端连接安全token,默认为空
     */
    private String           securityToken  = "";
    /**
     * 服务器连接地址
     */
    private String           endpoints;
    /**
     * 请求超时时间
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration         requestTimeout = Duration.ofSeconds(3);
    /**
     * 生产者配置信息
     */
    private RocketmqProducer publisher      = new RocketmqProducer();
    /**
     * 消费者配置信息
     */
    private RocketmqConsumer subscriber     = new RocketmqConsumer();

    @Data
    public static class RocketmqProducer {
        /**
         * 消息发送最大重试次数，默认-3次
         */
        private Integer maxAttempts = 3;

    }

    @Data
    public static class RocketmqConsumer {
        /**
         * 消费者分组group
         */
        private String  group          = "";
        /**
         * 消费者消费线程数
         */
        private Integer consumeThreads = 20;
        /**
         * 消费消息缓存数量,默认-1024条
         */
        private Integer maxCacheCount  = 1024;
        /**
         * 消费消息缓存容量,默认-64MB
         */
        private Integer maxCacheBytes  = 64 * 1024 * 1024;
    }

}
