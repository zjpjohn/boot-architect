package com.cloud.arch.event.props;

import lombok.Data;

@Data
public class PulsarMqProperties {

    private String         enpoints;
    private Integer        ioThreads               = 10;
    private Integer        listenerThreads         = 10;
    private boolean        enableTcpNoDelay        = false;
    /**
     * 单位秒
     */
    private Integer        keepAliveInterval       = 20;
    /**
     * 连接超时时间，单位秒
     */
    private Integer        connectionTimeout       = 10;
    /**
     * 操作超时时间，单位秒
     */
    private Integer        operationTimeout        = 15;
    /**
     * 单位毫秒
     */
    private Integer        startingBackoffInterval = 100;
    /**
     * 单位秒
     */
    private Integer        maxBackoffInterval      = 10;
    private String         consumerNameDelimiter   = "";
    private String         namespace               = "default";
    private String         tenant                  = "public";
    /**
     * 生产者配置信息
     */
    private PulsarProducer publisher                = new PulsarProducer();
    /**
     * 消费者配置信息
     */
    private PulsarConsumer subscriber                = new PulsarConsumer();

    @Data
    public static class PulsarProducer {

        /**
         * 发送消息超时时间，单位毫秒
         */
        private Integer sendTimeout        = 30;
        /**
         * 等待消费最大消息数量
         */
        private Integer maxPendingMessages = 1000;
    }

    @Data
    public static class PulsarConsumer {

        /**
         * 订阅者名称
         */
        private String  group;
        /**
         * 消费者队列容量
         */
        private Integer receiverQueueSize          = 1000;
        /**
         * 消费者确认超时时间，单位毫秒
         */
        private Long    ackTimeoutMillis           = 0L;
        /**
         * 消费分组确认超时时间，单位毫秒
         */
        private Long    acknowledgeGroupTime       = 100L;
        /**
         * 消费失败重投递时间，单位秒
         */
        private Long    negativeAckRedeliveryDelay = 60L;

    }

}
