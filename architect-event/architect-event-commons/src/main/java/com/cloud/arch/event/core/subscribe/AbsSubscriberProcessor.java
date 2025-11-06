package com.cloud.arch.event.core.subscribe;

import com.cloud.arch.event.annotations.Subscribe;
import com.cloud.arch.event.annotations.Subscribes;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringValueResolver;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbsSubscriberProcessor
        implements SmartInitializingSingleton, EmbeddedValueResolverAware, Ordered {

    private static final Class<Subscribes> CONTAINER_CLASS  = Subscribes.class;
    private static final Class<Subscribe>  ANNOTATION_CLASS = Subscribe.class;

    private StringValueResolver resolver;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 注册事件监听器
     *
     * @param metadataList 事件监听器元数据
     */
    public abstract void registerListeners(List<SubscribeEventMetadata> metadataList);

    @Override
    public void afterSingletonsInstantiated() {
        List<SubscribeEventMetadata> metadataList = this.scanListeners();
        this.registerListeners(metadataList);
    }

    /**
     * 扫描@Subscribe事件监听器
     */
    public List<SubscribeEventMetadata> scanListeners() {
        Set<Class<?>> allClass = Sets.newHashSet();
        allClass.addAll((Set<Class<?>>) ClassIndex.getAnnotated(CONTAINER_CLASS));
        allClass.addAll((Set<Class<?>>) ClassIndex.getAnnotated(ANNOTATION_CLASS));
        return allClass.stream().flatMap(clazz -> {
            Set<Subscribe> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(clazz,
                                                                                               ANNOTATION_CLASS,
                                                                                               CONTAINER_CLASS);
            return annotations.stream().map(annotation -> Pair.of(clazz, annotation));
        }).map(pair -> {
            Subscribe annotation = pair.getValue();
            SubscribeEventMetadata registrationMeta = new SubscribeEventMetadata(this.resolve(annotation.name()),
                                                                                 pair.getKey());
            return registrationMeta.group(this.resolve(annotation.group()))
                                   .filter(this.resolve(annotation.filter()))
                                   .sharding(annotation.sharding())
                                   .key(annotation.key());
        }).collect(Collectors.toList());
    }

    private String resolve(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return this.resolver.resolveStringValue(value);
    }
}
