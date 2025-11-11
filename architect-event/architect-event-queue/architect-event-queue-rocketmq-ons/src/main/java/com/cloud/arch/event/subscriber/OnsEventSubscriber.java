package com.cloud.arch.event.subscriber;

import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.OnsQueueProperties;
import com.cloud.arch.event.publisher.OnsEventPublisher;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class OnsEventSubscriber implements InitializingBean, DisposableBean, Ordered {

    public static final String CLUSTERING_MODEL        = "CLUSTERING";
    public static final String COMPOSITE_TAG_DELIMITER = "||";

    private final String                       group;
    private final OnsQueueProperties           properties;
    private final EventCodec                   eventCodec;
    private final List<SubscribeEventMetadata> registrations;
    private final SubscribeHandler             subscribeHandler;

    private Consumer consumer;

    public OnsEventSubscriber(String group,
                              OnsQueueProperties properties,
                              EventCodec eventCodec,
                              SubscribeHandler subscribeHandler,
                              List<SubscribeEventMetadata> registrations) {
        this.group            = group;
        this.properties       = properties;
        this.eventCodec       = eventCodec;
        this.subscribeHandler = subscribeHandler;
        this.registrations    = registrations;
    }

    /**
     * 创建消息消费者
     */
    private void createAndStartConsumer() {
        Properties props = new Properties();
        props.put(PropertyKeyConst.AccessKey, properties.getAccessKey());
        props.put(PropertyKeyConst.SecretKey, properties.getSecretKey());
        props.put(PropertyKeyConst.NAMESRV_ADDR, properties.getOnsAddress());
        props.put(PropertyKeyConst.MessageModel, CLUSTERING_MODEL);
        props.put(PropertyKeyConst.GROUP_ID, group);
        props.put(PropertyKeyConst.ConsumeTimeout, properties.getSubscriber().getConsumerTimeout());
        props.put(PropertyKeyConst.ConsumeThreadNums, properties.getSubscriber().getConsumeThreads());
        props.put(PropertyKeyConst.MaxReconsumeTimes, properties.getSubscriber().getMaxReconsumeTimes());
        // 创建消费者实例
        this.consumer = ONSFactory.createConsumer(props);
        // 解析注册消息监听器
        this.parseRegisterListeners();
        // 启动消费者实例
        this.consumer.start();
    }

    /**
     * 解析注册消息监听器
     */
    private void parseRegisterListeners() {
        Map<String, List<SubscribeEventMetadata>> namedRegistrations = this.registrations.stream()
                                                                                         .collect(Collectors.groupingBy(
                                                                                                 SubscribeEventMetadata::getName));
        for (Map.Entry<String, List<SubscribeEventMetadata>> entry : namedRegistrations.entrySet()) {
            String topic = entry.getKey();
            Assert.state(StringUtils.hasText(topic), "消息topic不允许为空.");
            Map<String, SubscribeEventMetadata> typeMapping = Maps.newHashMap();
            entry.getValue().forEach(registration -> {
                String tagRegex = registration.getFilter();
                // 消息过滤tag配置规则:不允许为空、不允许为'*'、不允许包含'||'
                boolean tagValidation = StringUtils.hasText(tagRegex) && !OnsEventPublisher.ONS_ALL_TAG_REGEX.equals(
                        tagRegex) && !tagRegex.contains(COMPOSITE_TAG_DELIMITER);
                Assert.state(tagValidation, "请配置具有业务意义的消息tag.");
                Assert.state(!typeMapping.containsKey(tagRegex), "同一topic消息主题下不允许配置相同tag.");
                // 同一个topic下的tag与事件类型映射
                typeMapping.put(tagRegex, registration);
            });
            String               compositeTag = String.join(COMPOSITE_TAG_DELIMITER, typeMapping.keySet());
            EventMessageListener listener     = new EventMessageListener(typeMapping, eventCodec, subscribeHandler);
            this.consumer.subscribe(topic, compositeTag, listener);
        }
    }

    /**
     * 消费订阅者标识，以消费者群组作为标识
     */
    public String identity() {
        return this.group;
    }

    @Override
    public void destroy() throws Exception {
        this.consumer.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.createAndStartConsumer();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
