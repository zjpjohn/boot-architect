package com.cloud.arch.rocket.commons;

import com.cloud.arch.rocket.utils.RocketmqConstants;
import lombok.Data;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "com.cloud.rocket.v4x")
public class RocketmqProperties {
    private String                     nameSrv;
    private String                     accessKey;
    private String                     secretKey;
    //rocketmq channel:LOCAL,CLOUD
    private String                     accessChannel                 = "LOCAL";
    private int                        clientCallbackExecutorThreads = 4;
    private int                        pollNameServerInterval        = 30000;
    private int                        heartbeatBrokerInterval       = 30000;
    private int                        persistConsumerOffsetInterval = 5000;
    private RocketmqProducerProperties producer;
    private RocketmqConsumerProperties consumer;

    @Data
    public static class RocketmqProducerProperties {
        /**
         * 是否开启rocketmq生产者
         */
        private boolean enable                        = false;
        /**
         * 注解@Peoducer类扫描包名，不配置为当前项目包名
         */
        private String  basePackages;
        /**
         * 是否开启事物消息
         */
        private boolean transaction                   = false;
        /*
         * 普通消息生产者group
         */
        private String  group                         = "DEFAULT_PRODUCER";
        /*
         *事物消息生产者group
         *
         */
        private String  txGroup                       = "";
        /**
         * 默认延迟消息推送到指定的队列
         */
        private String  delayTopic                    = "cloud_rocket_delay";
        /**
         * 发送消息超时时间：默认-3秒
         */
        private int     sendMessageTimeout            = 3000;
        /**
         * 消息题超过(默认:4k)开始压缩
         */
        private int     compressMsgBodyThrottle       = 1024 * 4;
        /**
         * 同步消息发送失败重试次数,默认-2次
         */
        private int     retryTimesWhenSendFailed      = 2;
        /**
         * 异步消息发送失败重试次数,默认-2次
         */
        private int     retryTimesWhenSendAsyncFailed = 2;
        /**
         * 发送失败是否重试其他broker
         */
        private boolean retryNextServer               = false;
        /**
         * 客户端限制消息长度(默认-4mb)
         */
        private int     maxMessageSize                = 1024 * 1024 * 4;
        /**
         * 是否开启消息追踪
         */
        private boolean enableTrace                   = true;
        /**
         * 自定义消息追踪topic
         */
        private String  traceTopic                    = TopicValidator.RMQ_SYS_TRACE_TOPIC;
        /**
         * 事务检查事件默认7天回收一次，回收cron表达式
         */
        private String  cleanCron                     = RocketmqConstants.DEFAULT_CLEAN_CRON;
        /**
         * 默认为每隔7天进行事务状态表数据清理
         */
        private Integer cleanInterval                 = RocketmqConstants.DEFAULT_INTERVAL;
        /**
         * 事务检查线程池核心线程数
         */
        private int     checkThreadPoolMinSize        = 1;
        /**
         * 事务检查线程池最大线程数
         */
        private int     checkThreadPoolMaxSize        = 1;
        /**
         * 线程池请求缓存队列，默认2000
         */
        private int     checkRequestHoldMax           = 2000;
        /**
         * 线程池线程保活时间，默认60秒
         */
        private long    keepAliveTime                 = 60000;
    }

    @Data
    public static class RocketmqConsumerProperties {

        /**
         * 是否开启rocketmq消费者
         */
        private boolean enable            = false;
        /**
         * 消费者分组，@Consumer()中group优先级最高
         */
        private String  group;
        /**
         * 是否开启消息消费轨迹
         */
        private boolean enableTrace       = true;
        /**
         * 消息规则topic
         */
        private String  traceTopic        = TopicValidator.RMQ_SYS_TRACE_TOPIC;
        /**
         * 是否开启消费者幂等检查
         */
        private boolean idempotent        = false;
        /**
         * 幂等检查时间默认7天回收一次
         */
        private String  cleanCron         = RocketmqConstants.DEFAULT_CLEAN_CRON;
        /**
         * 默认为每隔7天进行幂等表数据清理
         */
        private Integer cleanInterval     = RocketmqConstants.DEFAULT_INTERVAL;
        /**
         * 消费者线程池最大线程数量
         */
        private int     consumerThreadMax = 8;
        /**
         * 消费者线程池核心线程数量
         */
        private int     consumerThreadMin = 8;
        /**
         * 消费者消费超时时间
         */
        private long    consumerTimeout   = 15L;
    }
}
