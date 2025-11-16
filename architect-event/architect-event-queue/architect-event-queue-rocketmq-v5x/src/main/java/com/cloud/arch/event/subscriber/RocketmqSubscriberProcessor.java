package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.AbsSubscriberProcessor;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.RocketmqProperties;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class RocketmqSubscriberProcessor extends AbsSubscriberProcessor implements ApplicationContextAware {

    private final EventCodec         eventCodec;
    private final RocketmqProperties properties;

    private ApplicationContext context;

    public RocketmqSubscriberProcessor(EventCodec eventCodec, RocketmqProperties properties) {
        this.eventCodec = eventCodec;
        this.properties = properties;
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
        Map<String, List<SubscribeEventMetadata>> registrationMap = resolveMetadata(metadataList);
        // 事件实际处理bean
        SubscribeHandler          subscribeHandler = this.context.getBean(SubscribeHandler.class);
        GenericApplicationContext appContext       = (GenericApplicationContext) context;
        for (Map.Entry<String, List<SubscribeEventMetadata>> entry : registrationMap.entrySet()) {
            RocketEventSubscriber subscriber = new RocketEventSubscriber(entry.getKey(),
                                                                         eventCodec,
                                                                         properties,
                                                                         entry.getValue(),
                                                                         subscribeHandler);
            appContext.registerBean(subscriber.identity(), RocketEventSubscriber.class, () -> subscriber);
            appContext.getBean(subscriber.identity(), RocketEventSubscriber.class);
        }
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
