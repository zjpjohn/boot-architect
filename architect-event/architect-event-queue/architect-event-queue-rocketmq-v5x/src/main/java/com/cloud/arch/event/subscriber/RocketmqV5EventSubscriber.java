package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.RocketmqV5Properties;
import com.cloud.arch.event.utils.RocketmqV5Util;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cloud.arch.event.utils.RocketmqV5Util.COMPOSITE_TAG_DELIMITER;


@Slf4j
public class RocketmqV5EventSubscriber implements InitializingBean, DisposableBean {

    private final String                       group;
    private final EventCodec                   eventCodec;
    private final RocketmqV5Properties         properties;
    private final SubscribeHandler             handler;
    private final List<SubscribeEventMetadata> registrations;

    private PushConsumer consumer;

    public RocketmqV5EventSubscriber(String group,
                                     EventCodec eventCodec,
                                     RocketmqV5Properties properties,
                                     SubscribeHandler handler,
                                     List<SubscribeEventMetadata> registrations) {
        this.group         = group;
        this.eventCodec    = eventCodec;
        this.properties    = properties;
        this.handler       = handler;
        this.registrations = registrations;
    }

    public String identity() {
        return this.group;
    }

    @Override
    public void destroy() throws Exception {
        if (this.consumer != null) {
            this.consumer.close();
        }
    }

    private HashBasedTable<String, String, SubscribeEventMetadata> parseListenerMappings() {
        HashBasedTable<String, String, SubscribeEventMetadata> table = HashBasedTable.create();
        for (SubscribeEventMetadata registration : registrations) {
            String topic = registration.getName();
            Assert.state(StringUtils.isNotBlank(topic), "消息topic不允许为空.");
            String filterTag = registration.getFilter();
            // 消息过滤tag配置规则:不允许为空、不允许为'*'、不允许包含'||'
            boolean tagValidation = StringUtils.isNotBlank(filterTag)
                                    && !RocketmqV5Util.FILTER_TAG_REGEX.equals(filterTag)
                                    && !filterTag.contains(COMPOSITE_TAG_DELIMITER);
            Assert.state(tagValidation, "请配置局有业务意义的消息tag.");
            Assert.state(table.contains(topic, filterTag), "同一消息主题topic下不允许配置相同tag.");
            table.put(topic, filterTag, registration);
        }
        return table;
    }

    private void createRocketmqConsumer() throws ClientException {
        ClientConfiguration configuration = RocketmqV5Util.createConfiguration(properties);
        HashBasedTable<String, String, SubscribeEventMetadata> mappings       = this.parseListenerMappings();
        Map<String, FilterExpression>                          tagExpressions = Maps.newHashMap();
        for (Table.Cell<String, String, SubscribeEventMetadata> cell : mappings.cellSet()) {
            String tagExpression = Objects.requireNonNull(cell.getColumnKey());
            tagExpressions.put(cell.getRowKey(), new FilterExpression(tagExpression, FilterExpressionType.TAG));
        }
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        this.consumer = provider.newPushConsumerBuilder().setClientConfiguration(configuration)
                                .setConsumerGroup(this.group).setSubscriptionExpressions(tagExpressions)
                                .setMessageListener(new EventMessageListener(eventCodec, handler, mappings)).build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.createRocketmqConsumer();
    }

}
