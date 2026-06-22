package com.cloud.arch.event.props;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.apache.rocketmq.remoting.RPCHook;

import java.util.Objects;

@Data
public class RocketmqProperties {

    private String           nameSrv;
    private String           accessKey;
    private String           secretKey;
    private String           accessChannel                 = "LOCAL";
    private int              clientCallbackExecutorThreads = 4;
    private int              pollNameServerInterval        = 30000;
    private int              heartbeatBrokerInterval       = 30000;
    private int              persistConsumerOffsetInterval = 5000;
    private RocketmqProducer publisher                     = new RocketmqProducer();
    private RocketmqConsumer subscriber                    = new RocketmqConsumer();

    public RPCHook rpcHook() {
        if (StringUtils.isBlank(this.accessKey) || StringUtils.isBlank(this.secretKey)) {
            return null;
        }
        return new AclClientRPCHook(new SessionCredentials(this.accessKey, this.secretKey));
    }

    public AccessChannel accessChannel() {
        String channel = Objects.toString(this.accessChannel, "LOCAL");
        return AccessChannel.valueOf(channel);
    }

    @Data
    public static class RocketmqProducer {
        /**
         * 普通消息生产者group
         */
        private String  group                    = "DEFAULT_PRODUCER";
        /**
         * 发送消息超时时间：默认-3000ms
         */
        private int     sendMessageTimeout       = 5000;
        /**
         * 消息题超过(默认:4k)开始压缩
         */
        private int     compressMsgBodyThrottle  = 1024 * 4;
        /**
         * 同步消息发送失败重试次数,默认-2次
         */
        private int     retryTimesWhenSendFailed = 2;
        /**
         * 是否支持批量发送
         */
        private boolean enableBatch              = false;
        /**
         * 发送失败是否重试其他broker
         */
        private boolean retryNextServer          = false;
        /**
         * 客户端限制消息长度(默认-4mb)
         */
        private int     maxMessageSize           = 1024 * 1024 * 4;
        /**
         * 是否开启消息追踪
         */
        private boolean enableTrace              = true;
        /**
         * 自定义消息追踪topic
         */
        private String  traceTopic               = TopicValidator.RMQ_SYS_TRACE_TOPIC;
    }

    @Data
    public static class RocketmqConsumer {
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
         * 消费者线程池最大线程数量
         */
        private int     consumerThreadMax = 16;
        /**
         * 消费者线程池核心线程数量
         */
        private int     consumerThreadMin = 16;
        /**
         * 消费者消费超时时间
         */
        private long    consumerTimeout   = 15L;
    }

}
