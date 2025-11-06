package com.cloud.arch.rocket.producer.spring;

import com.cloud.arch.rocket.annotations.Sender;
import com.cloud.arch.rocket.producer.core.OnsProducerTemplate;
import com.cloud.arch.rocket.producer.core.OnsRecogniseHandler;
import com.cloud.arch.rocket.producer.core.OnsSendHandler;
import com.cloud.arch.rocket.utils.MethodUtils;
import com.google.common.collect.Maps;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProxyProducerProvider implements EmbeddedValueResolverAware {

    private final Map<Class<?>, OnsMethodsSendHandler> senders = Maps.newConcurrentMap();
    private final OnsProducerTemplate                  producerTemplate;
    private final OnsRecogniseHandler                  recogniseHandler;
    private       StringValueResolver                  resolver;

    public ProxyProducerProvider(OnsProducerTemplate producerTemplate, OnsRecogniseHandler recogniseHandler) {
        this.producerTemplate = producerTemplate;
        this.recogniseHandler = recogniseHandler;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    public InvocationHandler newInstance(Class<?> type) {
        return senders.computeIfAbsent(type, key -> {
            Map<Method, OnsSendHandler> handlers = this.getSendHandlers(key);
            return new OnsMethodsSendHandler(handlers);
        });
    }

    private Map<Method, OnsSendHandler> getSendHandlers(Class<?> type) {
        return Arrays.stream(type.getMethods())
                     .filter(m -> !this.shouldFilterMethod(m))
                     .filter(m -> m.getAnnotation(Sender.class) != null)
                     .map(m -> new OnsSendHandler(m, resolver, producerTemplate, recogniseHandler))
                     .peek(OnsSendHandler::validate)
                     .collect(Collectors.toMap(key -> key.getMetadata().getMethod(), Function.identity()));
    }

    private boolean shouldFilterMethod(Method method) {
        return method.getDeclaringClass() == Object.class
                || (method.getModifiers() & Modifier.STATIC) != 0
                || MethodUtils.isDefault(method);
    }

}
