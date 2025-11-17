package com.cloud.arch.rocket.producer.spring;

import com.cloud.arch.rocket.annotations.Sender;
import com.cloud.arch.rocket.producer.core.RocketProducerTemplate;
import com.cloud.arch.rocket.producer.core.RocketRecogniseHandler;
import com.cloud.arch.rocket.producer.core.RocketSendHandler;
import com.cloud.arch.rocket.utils.MethodUtils;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import com.google.common.collect.Maps;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProxyProducerProvider implements EmbeddedValueResolverAware, ApplicationContextAware {

    private final RocketProducerTemplate                  producerTemplate;
    private final Map<Class<?>, RocketMethodsSendHandler> senders = Maps.newConcurrentMap();
    private       StringValueResolver                     resolver;
    private       ApplicationContext                      context;

    public ProxyProducerProvider(RocketProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public InvocationHandler newInstance(Class<?> type) {
        RocketMethodsSendHandler handler = senders.get(type);
        if (handler != null) {
            return handler;
        }
        RocketRecogniseHandler recogniseHandler = context.getBean(RocketmqUtils.SENDER_RECOGNISE_BEAN_NAME,
                                                                  RocketRecogniseHandler.class);
        Map<Method, RocketSendHandler> handlers = Arrays.stream(type.getMethods())
                                                        .filter(m -> !this.shouldFilterMethod(m))
                                                        .filter(m -> m.getAnnotation(Sender.class) != null)
                                                        .map(m -> new RocketSendHandler(m,
                                                                                        producerTemplate,
                                                                                        resolver,
                                                                                        recogniseHandler))
                                                        .peek(RocketSendHandler::validate)
                                                        .collect(Collectors.toMap(key -> key.getMetadata().getMethod(),
                                                                                  Function.identity()));
        RocketMethodsSendHandler sendHandler = new RocketMethodsSendHandler(handlers);
        return this.senders.put(type, sendHandler);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    private boolean shouldFilterMethod(Method method) {
        return method.getDeclaringClass() == Object.class
                || (method.getModifiers() & Modifier.STATIC) != 0
                || MethodUtils.isDefault(method);
    }
}
