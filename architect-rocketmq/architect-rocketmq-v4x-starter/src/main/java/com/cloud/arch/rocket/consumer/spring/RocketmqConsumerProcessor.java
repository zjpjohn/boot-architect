package com.cloud.arch.rocket.consumer.spring;

import com.cloud.arch.rocket.annotations.Consumer;
import com.cloud.arch.rocket.annotations.Listener;
import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.consumer.core.ListenerMetadata;
import com.cloud.arch.rocket.domain.MessageModel;
import com.cloud.arch.rocket.idempotent.Idempotent;
import com.cloud.arch.rocket.idempotent.IdempotentChecker;
import com.cloud.arch.rocket.serializable.Serialize;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RocketmqConsumerProcessor
        implements ApplicationContextAware, SmartInitializingSingleton, EmbeddedValueResolverAware, Ordered {

    private final RocketmqProperties  properties;
    private final Serialize           serialize;
    private       ApplicationContext  context;
    private       StringValueResolver resolver;

    public RocketmqConsumerProcessor(RocketmqProperties properties, Serialize serialize) {
        this.properties = properties;
        this.serialize  = serialize;
    }

    private void initialize() {
        GenericApplicationContext appContext = (GenericApplicationContext) this.context;
        this.context.getBeansWithAnnotation(Consumer.class).entrySet().stream()
                    .filter(v -> !ScopedProxyUtils.isScopedTarget(v.getKey())).flatMap(entry -> {
                Consumer     annotation = this.context.findAnnotationOnBean(entry.getKey(), Consumer.class);
                String       group      = resolver.resolveStringValue(annotation.group());
                final String groupId    = StringUtils.isNotBlank(group) ? group : properties.getConsumer().getGroup();
                Assert.state(StringUtils.isNotBlank(groupId), "请配置消息消费群组group.");
                return parseGroupMetaList(group, annotation.model(), entry.getValue());
            }).collect(Collectors.groupingBy(meta -> Pair.of(meta.getGroup(), meta.getModel()))).forEach((pair, value) -> {
                RocketmqConsumerContainer container
                        = new RocketmqConsumerContainer(pair.getKey(), pair.getValue(), serialize, properties, value);
                appContext.registerBean(container.identity(), RocketmqConsumerContainer.class, () -> container);
                appContext.getBean(container.identity(), RocketmqConsumerContainer.class);
            });
    }

    /**
     * 获取@Consumer注解下的listener元数据
     *
     * @param group group信息
     * @param model 发送消息model
     * @param bean  @Consumer注解的bean
     */
    private Stream<ListenerMetadata> parseGroupMetaList(String group, MessageModel model, Object bean) {
        Class<?> userType = ClassUtils.getUserClass(bean);
        return MethodIntrospector.selectMethods(userType, (MethodIntrospector.MetadataLookup<Listener>) method -> AnnotatedElementUtils.getMergedAnnotation(method, Listener.class))
                                 .entrySet().stream().map(listener -> {
                    Method            method            = AopUtils.selectInvocableMethod(listener.getKey(), userType);
                    Idempotent        idempotent        = listener.getValue().idempotent();
                    IdempotentChecker idempotentChecker = getIdempotentChecker(idempotent);
                    return new ListenerMetadata(group, model, bean, method, listener.getValue(), resolver, idempotentChecker);
                });
    }

    /**
     * 获取幂等校验bean
     *
     * @param idempotent 幂等类型
     */
    public IdempotentChecker getIdempotentChecker(Idempotent idempotent) {
        if (!this.properties.getConsumer().isIdempotent() || StringUtils.isBlank(idempotent.getName())) {
            return null;
        }
        try {
            return this.context.getBean(idempotent.getName(), IdempotentChecker.class);
        } catch (BeansException e) {
            return null;
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.initialize();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
