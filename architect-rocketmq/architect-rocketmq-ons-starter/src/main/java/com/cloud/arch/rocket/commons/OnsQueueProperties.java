package com.cloud.arch.rocket.commons;

import com.cloud.arch.rocket.utils.RocketmqUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "com.cloud.rocket.ons")
public class OnsQueueProperties {

    /**
     * 阿里云消息队列accessKey
     */
    private String                accessKey;
    /**
     * 阿里云消息队列secretKey
     */
    private String                secretKey;
    /**
     * 阿里云消息队列地址
     */
    private String                onsAddress;
    /**
     * 消息生产者配置信息
     */
    private OnsProducerProperties producer;
    /**
     * 消息消费者配置信息
     */
    private OnsConsumerProperties consumer;

    @Data
    public static class OnsProducerProperties {

        //是否启用生产者
        private boolean enable        = false;
        //发送超时时间
        private Long    timeout       = 3000L;
        //扫描@Producer包集合
        private String  basePackages;
        //是否开启事物消息
        private boolean transaction   = false;
        //是否开启顺序消息
        private boolean ordered       = false;
        //默认7天前的事务状态表数据清理
        private Integer cleanInterval = RocketmqUtils.DEFAULT_INTERVAL;
        //事务状态回收cron表达式
        private String  cleanCron     = RocketmqUtils.DEFAULT_CLEAN_CRON;
    }

    @Data
    public static class OnsConsumerProperties {

        //是否启用消费者
        private boolean enable         = false;
        //消费者group分组
        private String  group;
        //消费者超时时间(单位分钟)
        private Long    consumeTimeout = 15L;
        //是否开启消费者消息幂等
        private boolean idempotent     = false;
        //默认7天前的幂等信息表数据清理
        private Integer cleanInterval  = RocketmqUtils.DEFAULT_INTERVAL;
        //幂等信息回收cron表达式
        private String  cleanCron      = RocketmqUtils.DEFAULT_CLEAN_CRON;

    }
}
