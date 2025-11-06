package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.AbsSubscriberProcessor;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.OnsQueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class OnsSubscriberProcessor extends AbsSubscriberProcessor implements ApplicationContextAware {

    private final OnsQueueProperties properties;
    private final EventCodec         eventCodec;
    private       ApplicationContext context;

    public OnsSubscriberProcessor(OnsQueueProperties properties, EventCodec eventCodec) {
        this.properties = properties;
        this.eventCodec = eventCodec;
    }

    private String resolveGroup(SubscribeEventMetadata registration) {
        String group = registration.getGroup();
        if (StringUtils.isBlank(group)) {
            group = properties.getSubscriber().getGroup();
        }
        Assert.state(StringUtils.isNotBlank(group), "请配置消费者群组");
        return group;
    }

    /**
     * 注册事件监听器
     *
     * @param metadataList 事件监听器元数据
     */
    @Override
    public void registerListeners(List<SubscribeEventMetadata> metadataList) {
        Map<String, List<SubscribeEventMetadata>> registrationMap = metadataList.stream()
                                                                                .distinct()
                                                                                .peek(registration -> {
                                                                                    String group = resolveGroup(
                                                                                            registration);
                                                                                    registration.group(group);
                                                                                })
                                                                                .collect(Collectors.groupingBy(
                                                                                        SubscribeEventMetadata::getGroup));
        // 事件实际处理bean
        SubscribeHandler          subscribeHandler = this.context.getBean(SubscribeHandler.class);
        GenericApplicationContext appContext       = (GenericApplicationContext) context;
        for (Map.Entry<String, List<SubscribeEventMetadata>> entry : registrationMap.entrySet()) {
            List<SubscribeEventMetadata> listenerRegistrations = entry.getValue();
            OnsEventSubscriber subscriber = new OnsEventSubscriber(entry.getKey(),
                                                                   properties,
                                                                   eventCodec,
                                                                   subscribeHandler,
                                                                   listenerRegistrations);
            // 注册订阅者bean
            appContext.registerBean(subscriber.identity(), OnsEventSubscriber.class, () -> subscriber);
            appContext.getBean(subscriber.identity(), OnsEventSubscriber.class);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
