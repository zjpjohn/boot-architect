package com.cloud.arch.rocket.consumer.spring;

import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.consumer.core.ListenerMetadata;
import com.cloud.arch.rocket.consumer.core.SingleMessageListener;
import com.cloud.arch.rocket.domain.MessageModel;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.utils.RocketOnsConstants;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


@Slf4j
public class OnsConsumerContainer implements InitializingBean, DisposableBean, Ordered {

    private final OnsQueueProperties     properties;
    private final String                 group;
    private final MessageModel           model;
    private final List<ListenerMetadata> metaList;
    private final Serialize              serialize;
    private       Consumer               consumer;

    public OnsConsumerContainer(OnsQueueProperties properties,
                                String group,
                                MessageModel model,
                                Serialize serialize,
                                List<ListenerMetadata> metaList) {
        this.properties = properties;
        this.model      = model;
        this.group      = group;
        this.serialize  = serialize;
        this.metaList   = metaList;
    }

    /**
     * 消费者唯一标识
     */
    public String identity() {
        return group + "_" + model.getModel();
    }

    /**
     * 创建消息消费者
     */
    private void createAndRegisterConsumer() {
        Properties props = new Properties();
        props.put(PropertyKeyConst.AccessKey, properties.getAccessKey());
        props.put(PropertyKeyConst.SecretKey, properties.getSecretKey());
        props.put(PropertyKeyConst.NAMESRV_ADDR, properties.getOnsAddress());
        props.put(PropertyKeyConst.MessageModel, model.getModel());
        props.put(PropertyKeyConst.ConsumeTimeout, properties.getConsumer().getConsumeTimeout());
        props.put(PropertyKeyConst.GROUP_ID, group);
        //创建消费者实例
        this.consumer = ONSFactory.createConsumer(props);
        //解析并注册消息监听器
        this.parseAndRegisterListeners();
        //启动消费者
        this.consumer.start();
    }

    /**
     * 解析注册消息监听器
     */
    private void parseAndRegisterListeners() {
        //将监听器元数据信息按topic进行分组
        Map<String, List<ListenerMetadata>> listMap = this.metaList.stream()
                                                                   .collect(Collectors.groupingBy(ListenerMetadata::topic));
        for (Map.Entry<String, List<ListenerMetadata>> entry : listMap.entrySet()) {
            String                        topic       = entry.getKey();
            Map<String, ListenerMetadata> metadataMap = Maps.newHashMap();
            entry.getValue().forEach(metadata -> {
                String listenerTag = metadata.tag();
                Assert.state(!metadataMap.containsKey(listenerTag), "同一个topic消费主题下不允许存在相同tag的Listener.");
                metadataMap.put(listenerTag, metadata);
            });
            String                compositeTag    = String.join(RocketOnsConstants.ONS_COMPOSITE_TAG_DELIMITER, metadataMap.keySet());
            SingleMessageListener messageListener = new SingleMessageListener(metadataMap, serialize);
            this.consumer.subscribe(topic, compositeTag, messageListener);
        }
    }

    @Override
    public void destroy() throws Exception {
        this.consumer.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("rocketmq ons queue consumer[{}-{}] start...", this.group, this.model.getModel());
        this.createAndRegisterConsumer();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
