package com.cloud.arch.cache.interceptor.context;

import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

/**
 * 缓存操作上下文容器，按操作类型（CacheResult/CachePut/CacheEvict）分组存储 {@link OperationContext}，
 * 供 {@link com.cloud.arch.cache.interceptor.CacheAspectSupport} 在执行时按类型取用
 */
@SuppressWarnings("rawtypes")
public class CacheContextContainer {

    private final MultiValueMap<Class<? extends AbsCacheOperation>, OperationContext> contexts;

    public CacheContextContainer(Collection<? extends AbsCacheOperation<? extends Annotation>> operations, Function<AbsCacheOperation<? extends Annotation>, OperationContext> generator) {
        this.contexts = new LinkedMultiValueMap<>();
        operations.forEach(v -> {
            this.contexts.add(v.getClass(), generator.apply(v));
        });
    }

    public Collection<OperationContext> get(Class<? extends AbsCacheOperation> clazz) {
        return Optional.ofNullable(this.contexts.get(clazz)).orElse(Collections.emptyList());
    }

}
