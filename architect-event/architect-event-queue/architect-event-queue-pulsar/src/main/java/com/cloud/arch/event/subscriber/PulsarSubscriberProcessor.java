package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.AbsSubscriberProcessor;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.PulsarMqProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PulsarSubscriberProcessor extends AbsSubscriberProcessor implements ApplicationContextAware {

    private final PulsarMqProperties properties;
    private final EventCodec         eventCodec;
    private final PulsarClient       pulsarClient;

    private ApplicationContext applicationContext;

    public PulsarSubscriberProcessor(PulsarMqProperties properties, EventCodec eventCodec, PulsarClient pulsarClient) {
        this.properties   = properties;
        this.eventCodec   = eventCodec;
        this.pulsarClient = pulsarClient;
    }

    private void resolveGroup(SubscribeEventMetadata registration) {
        String group = registration.getGroup();
        if (StringUtils.isBlank(group)) {
            group = properties.getSubscriber().getGroup();
        }
        Assert.state(StringUtils.isNotBlank(group), "请配置消费者群组");
        registration.group(group);
    }

    /**
     * 注册事件监听器
     *
     * @param metadataList 事件监听器元数据
     */
    @Override
    public void registerListeners(List<SubscribeEventMetadata> metadataList) {
        Map<String, List<SubscribeEventMetadata>> metadataMap = metadataList.stream()
                                                                            .distinct()
                                                                            .peek(this::resolveGroup)
                                                                            .collect(Collectors.groupingBy(
                                                                                    SubscribeEventMetadata::getGroup));
        SubscribeHandler          subscribeHandler = this.applicationContext.getBean(SubscribeHandler.class);
        GenericApplicationContext appContext       = (GenericApplicationContext) this.applicationContext;
        for (Map.Entry<String, List<SubscribeEventMetadata>> entry : metadataMap.entrySet()) {
            List<SubscribeEventMetadata> metadata = entry.getValue();
            PulsarEventSubscriber subscriber = new PulsarEventSubscriber(entry.getKey(),
                                                                         eventCodec,
                                                                         properties,
                                                                         metadata,
                                                                         subscribeHandler,
                                                                         pulsarClient);
            appContext.registerBean(subscriber.identity(), PulsarEventSubscriber.class, () -> subscriber);
            appContext.getBean(subscriber.identity(), PulsarEventSubscriber.class);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
