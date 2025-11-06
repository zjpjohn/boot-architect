package com.cloud.arch.event.props;

import lombok.Data;

@Data
public class OnsQueueProperties {

    /**
     * 阿里云消息队列accessKey
     */
    private String      accessKey;
    /**
     * 阿里云消息队列secretKey
     */
    private String      secretKey;
    /**
     * 阿里云消息队列地址
     */
    private String      onsAddress;
    /**
     * 生产者配置
     */
    private OnsProducer publisher  = new OnsProducer();
    /**
     * 消费者配置
     */
    private OnsConsumer subscriber = new OnsConsumer();

    @Data
    public static class OnsProducer {
        /**
         * 生产者发送群组
         */
        private String  group               = "__ONS_PRODUCER_DEFAULT_GROUP";
        /**
         * 发送超时时间
         */
        private Integer sendTimeoutMillis   = 5000;
        /**
         * 是否开启精确投递语义
         */
        private boolean exactlyOnceDelivery = false;

    }

    @Data
    public static class OnsConsumer {
        /**
         * 消费者group分组
         */
        private String  group;
        /**
         * 消费者超时时间
         */
        private Integer consumerTimeout   = 15;
        /**
         * 消费者线程数
         */
        private Integer consumeThreads    = 16;
        /**
         * 最多重新消费次数（1~16次）
         */
        private Integer maxReconsumeTimes = 12;
    }

}
