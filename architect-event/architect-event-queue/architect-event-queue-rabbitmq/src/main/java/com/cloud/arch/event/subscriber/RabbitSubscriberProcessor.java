package com.cloud.arch.event.subscriber;

import com.cloud.arch.event.codec.EventCodec;
import com.cloud.arch.event.core.subscribe.AbsSubscriberProcessor;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import com.cloud.arch.event.props.RabbitmqProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;

public class RabbitSubscriberProcessor extends AbsSubscriberProcessor
        implements ApplicationContextAware, SmartInitializingSingleton {

    private final EventCodec                           eventCodec;
    private final Exchange                             exchange;
    private final AmqpAdmin                            amqpAdmin;
    private final DirectRabbitListenerContainerFactory containerFactory;
    private final RabbitListenerEndpointRegistry       endpointRegistry;

    private ApplicationContext context;

    public RabbitSubscriberProcessor(EventCodec eventCodec,
                                     RabbitmqProperties properties,
                                     AmqpAdmin amqpAdmin,
                                     DirectRabbitListenerContainerFactory containerFactory,
                                     RabbitListenerEndpointRegistry endpointRegistry) {
        this.eventCodec       = eventCodec;
        this.endpointRegistry = endpointRegistry;
        this.exchange         = ExchangeBuilder.directExchange(properties.getConsumer().getExchange()).build();
        this.containerFactory = containerFactory;
        this.amqpAdmin        = amqpAdmin;
    }

    /**
     * 注册事件监听器
     *
     * @param metadataList 事件监听器元数据
     */
    @Override
    public void registerListeners(List<SubscribeEventMetadata> metadataList) {
        SubscribeHandler subscribeHandler = this.context.getBean(SubscribeHandler.class);
        metadataList.stream()
                    .distinct()
                    .forEach(registration -> this.registrySubscriber(registration, subscribeHandler));
    }

    /**
     * 事件监听队列绑定
     */
    private void initSubscriber(SubscribeEventMetadata registration) {
        String queueName = registration.getName();
        if (StringUtils.isBlank(queueName)) {
            throw new IllegalArgumentException("消费队列名称为空，请配置消费队列名称.");
        }
        // 声明消息队列
        Queue eventQueue = QueueBuilder.durable(queueName).build();
        amqpAdmin.declareQueue(eventQueue);
        // 绑定消息队列与交换机
        Binding binding = BindingBuilder.bind(eventQueue).to(exchange).with(queueName).noargs();
        amqpAdmin.declareBinding(binding);
    }

    /**
     * 绑定事件处理器
     */
    private void registryHandler(SubscribeEventMetadata registration, SubscribeHandler subscribeHandler) {
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId(registration.getName());
        endpoint.setQueueNames(registration.getName());
        endpoint.setMessageListener(new RabbitEventListener(registration, eventCodec, subscribeHandler));
        endpointRegistry.registerListenerContainer(endpoint, containerFactory, true);
    }

    /**
     * 注册订阅者
     *
     * @param registration     订阅信息
     * @param subscribeHandler 实际业务处理器
     */
    private void registrySubscriber(SubscribeEventMetadata registration, SubscribeHandler subscribeHandler) {
        //初始化订阅绑定
        this.initSubscriber(registration);
        //绑定事件处理器
        this.registryHandler(registration, subscribeHandler);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

}
