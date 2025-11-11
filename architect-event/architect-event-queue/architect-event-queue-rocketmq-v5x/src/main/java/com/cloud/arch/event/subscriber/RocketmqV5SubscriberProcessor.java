package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.AbsSubscriberProcessor;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.RocketmqV5Properties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RocketmqV5SubscriberProcessor extends AbsSubscriberProcessor implements ApplicationContextAware {

    private final RocketmqV5Properties properties;
    private final EventCodec           eventCodec;

    private ApplicationContext applicationContext;

    public RocketmqV5SubscriberProcessor(RocketmqV5Properties properties, EventCodec eventCodec) {
        this.properties = properties;
        this.eventCodec = eventCodec;
    }

    private void resolveGroup(SubscribeEventMetadata registration) {
        String group = registration.getGroup();
        if (StringUtils.isBlank(group)) {
            group = properties.getSubscriber().getGroup();
        }
        Assert.state(StringUtils.isNotBlank(group), "请配置消费者群组");
        registration.group(group);
    }

    private Map<String, List<SubscribeEventMetadata>> resolveMetadata(List<SubscribeEventMetadata> metadataList) {
        return metadataList.stream()
                           .distinct()
                           .peek(this::resolveGroup)
                           .collect(Collectors.groupingBy(SubscribeEventMetadata::getGroup));
    }

    /**
     * 注册事件监听器
     *
     * @param metadataList 事件监听器元数据
     */
    @Override
    public void registerListeners(List<SubscribeEventMetadata> metadataList) {
        Map<String, List<SubscribeEventMetadata>> registrationMap  = resolveMetadata(metadataList);
        SubscribeHandler                          subscribeHandler = this.applicationContext.getBean(SubscribeHandler.class);
        GenericApplicationContext                 appContext       = (GenericApplicationContext) this.applicationContext;
        for (Map.Entry<String, List<SubscribeEventMetadata>> entry : registrationMap.entrySet()) {
            List<SubscribeEventMetadata> listenerRegistrations = entry.getValue();
            RocketmqV5EventSubscriber subscriber = new RocketmqV5EventSubscriber(entry.getKey(),
                                                                                 eventCodec,
                                                                                 properties,
                                                                                 subscribeHandler,
                                                                                 listenerRegistrations);
            appContext.registerBean(subscriber.identity(), RocketmqV5EventSubscriber.class, () -> subscriber);
            appContext.getBean(subscriber.identity(), RocketmqV5EventSubscriber.class);
        }
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
