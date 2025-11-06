package com.cloud.arch.event.publisher;

import com.cloud.arch.event.core.publish.EventMessage;
import com.cloud.arch.event.core.publish.EventMetadata;
import com.cloud.arch.event.core.publish.EventMetadataFactory;
import com.cloud.arch.event.core.publish.EventPublisher;
import com.cloud.arch.event.props.RocketmqV5Properties;
import com.cloud.arch.event.utils.RocketmqV5Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.java.message.MessageBuilderImpl;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloud.arch.event.utils.RocketmqV5Util.FILTER_TAG_REGEX;


public class RocketmqV5EventPublisher
        implements EventPublisher, DisposableBean, SmartInitializingSingleton, EmbeddedValueResolverAware {

    private final RocketmqV5Properties  properties;
    private final ClientServiceProvider provider;
    private       Producer              producer;
    private       StringValueResolver   resolver;

    public RocketmqV5EventPublisher(RocketmqV5Properties properties) {
        this.properties = properties;
        this.provider   = ClientServiceProvider.loadService();
    }

    /**
     * 发布跨应用领域事件
     *
     * @param message 事件消息
     */
    @Override
    public void publish(EventMessage message) {
        Message rocketMessage = this.checkAndConvert(message);
        try {
            this.producer.send(rocketMessage);
        } catch (ClientException error) {
            throw new RuntimeException(error.getMessage(), error);
        }
    }

    /**
     * 事件消息校验转换
     *
     * @param message 领域事件消息
     */
    public Message checkAndConvert(EventMessage message) {
        Assert.state(StringUtils.isNotBlank(message.getName()), "消息topic不允许为空");
        Assert.state(StringUtils.isNotBlank(message.getData()), "消息内容不允许为空");
        Assert.state(StringUtils.isNotBlank(message.getKey()), "消息业务key不允许为空");
        // 强制设置消息过滤tag不允许为空且不能为'*'
        String messageFilter = message.getFilter();
        Assert.state(StringUtils.isNotBlank(messageFilter)
                     && !FILTER_TAG_REGEX.equals(messageFilter), "消息过滤tag不允许为空，请根据业务设置具体过滤tag");
        byte[]             payload = message.getData().getBytes(StandardCharsets.UTF_8);
        MessageBuilderImpl builder = new MessageBuilderImpl();
        builder.setTopic(message.getName()).setTag(messageFilter).setKeys(message.getKey()).setBody(payload);
        Long delay = message.getDelay();
        if (delay != null && delay > 0) {
            builder.setDeliveryTimestamp(System.currentTimeMillis() + delay);
        }
        return builder.build();
    }

    @Override
    public void destroy() throws Exception {
        if (this.producer != null) {
            this.producer.close();
        }
    }

    private Set<String> getEventTopics() {
        Map<Class<?>, EventMetadata> metaMap = EventMetadataFactory.getMetaMap();
        return metaMap.values().stream().map(EventMetadata::getRemoteMetas).flatMap(table -> table.rowKeySet().stream())
                      .map(resolver::resolveStringValue).collect(Collectors.toSet());
    }

    private Producer createProducer() throws ClientException {
        ClientConfiguration configuration = RocketmqV5Util.createConfiguration(this.properties);
        return this.provider.newProducerBuilder().setClientConfiguration(configuration)
                            .setTopics(this.getEventTopics().toArray(new String[0]))
                            .setMaxAttempts(this.properties.getPublisher().getMaxAttempts()).build();
    }

    @Override
    public void setEmbeddedValueResolver(
            @Nonnull StringValueResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            this.producer = this.createProducer();
        } catch (ClientException error) {
            throw new RuntimeException(error.getMessage(), error);
        }
    }
}
