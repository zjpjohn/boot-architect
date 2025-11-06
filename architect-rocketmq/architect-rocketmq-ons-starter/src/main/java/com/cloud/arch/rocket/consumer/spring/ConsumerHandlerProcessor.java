package com.cloud.arch.rocket.consumer.spring;

import com.cloud.arch.rocket.annotations.Consumer;
import com.cloud.arch.rocket.annotations.Listener;
import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.consumer.core.ListenerMetadata;
import com.cloud.arch.rocket.domain.MessageModel;
import com.cloud.arch.rocket.idempotent.Idempotent;
import com.cloud.arch.rocket.idempotent.IdempotentChecker;
import com.cloud.arch.rocket.serializable.Serialize;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ConsumerHandlerProcessor
        implements ApplicationContextAware, SmartInitializingSingleton, EmbeddedValueResolverAware, Ordered {

    private final OnsQueueProperties  queueProperties;
    private final Serialize           serialize;
    private       ApplicationContext  context;
    private       StringValueResolver resolver;

    public ConsumerHandlerProcessor(OnsQueueProperties queueProperties, Serialize serialize) {
        this.queueProperties = queueProperties;
        this.serialize       = serialize;
    }


    /**
     * 初始化消息消费者容器
     */
    private void initialize() {
        Map<String, Object>       beans      = this.context.getBeansWithAnnotation(Consumer.class);
        GenericApplicationContext appContext = (GenericApplicationContext) this.context;
        beans.entrySet()
             .stream()
             .filter(entry -> !ScopedProxyUtils.isScopedTarget(entry.getKey()))
             .flatMap(entry -> {
                 Consumer annotation = this.context.findAnnotationOnBean(entry.getKey(), Consumer.class);
                 String   group      = resolver.resolveStringValue(annotation.group());
                 String groupId = Optional.ofNullable(group)
                                          .filter(StringUtils::isNotBlank)
                                          .orElse(queueProperties.getConsumer().getGroup());
                 Assert.state(StringUtils.isNotBlank(groupId), "请配置消息消费群组group.");
                 return parseGroupMetaList(groupId, annotation.model(), entry.getValue());
             })
             .collect(Collectors.groupingBy(metadata -> Pair.of(metadata.getGroup(), metadata.getModel())))
             .forEach((pair, value) -> {
                 OnsConsumerContainer container = new OnsConsumerContainer(queueProperties,
                                                                           pair.getKey(),
                                                                           pair.getValue(),
                                                                           serialize,
                                                                           value);
                 appContext.registerBean(container.identity(), OnsConsumerContainer.class, () -> container);
                 appContext.getBean(container.identity(), OnsConsumerContainer.class);
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
        return MethodIntrospector.selectMethods(userType,
                                                (MethodIntrospector.MetadataLookup<Listener>) method -> AnnotatedElementUtils.getMergedAnnotation(
                                                        method,
                                                        Listener.class)).entrySet().stream().map(listener -> {
            Method            method            = AopUtils.selectInvocableMethod(listener.getKey(), userType);
            Idempotent        idempotent        = listener.getValue().idempotent();
            IdempotentChecker idempotentChecker = getIdempotentChecker(idempotent);
            return new ListenerMetadata(group, model, bean, method, listener.getValue(), idempotentChecker, resolver);
        });
    }

    /**
     * 获取幂等校验bean
     *
     * @param idempotent 消费者幂等类型
     */
    public IdempotentChecker getIdempotentChecker(Idempotent idempotent) {
        if (!this.queueProperties.getConsumer().isIdempotent() || StringUtils.isBlank(idempotent.getName())) {
            return null;
        }
        try {
            return this.context.getBean(idempotent.getName(), IdempotentChecker.class);
        } catch (BeansException e) {
            if (this.queueProperties.getConsumer().isIdempotent() && StringUtils.isNotBlank(idempotent.getName())) {
                log.warn("idempotent checker bean not exist,please check spring configuration!");
            }
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
