package com.cloud.arch.cache.interceptor.context;

import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("rawtypes")
public class CacheContextContainer {

    private final MultiValueMap<Class<? extends AbsCacheOperation>, OperationContext> contexts;

    public CacheContextContainer(Collection<? extends AbsCacheOperation<? extends Annotation>> operations,
                                 Function<AbsCacheOperation<? extends Annotation>, OperationContext> generator) {
        this.contexts = new LinkedMultiValueMap<>();
        operations.forEach(v -> {
            this.contexts.add(v.getClass(), generator.apply(v));
        });
    }

    public Collection<OperationContext> get(Class<? extends AbsCacheOperation> clazz) {
        return Optional.ofNullable(this.contexts.get(clazz)).orElse(Collections.emptyList());
    }

}
