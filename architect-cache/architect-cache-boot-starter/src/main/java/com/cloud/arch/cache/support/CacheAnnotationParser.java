package com.cloud.arch.cache.support;

import com.cloud.arch.cache.annotations.CacheAction;
import com.cloud.arch.cache.annotations.CacheEvict;
import com.cloud.arch.cache.annotations.CachePut;
import com.cloud.arch.cache.annotations.CacheResult;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import com.cloud.arch.cache.interceptor.operation.CacheEvictOperation;
import com.cloud.arch.cache.interceptor.operation.CachePutOperation;
import com.cloud.arch.cache.interceptor.operation.CacheResultOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class CacheAnnotationParser {

    public static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(4);

    static {
        CACHE_OPERATION_ANNOTATIONS.add(CacheResult.class);
        CACHE_OPERATION_ANNOTATIONS.add(CachePut.class);
        CACHE_OPERATION_ANNOTATIONS.add(CacheEvict.class);
    }

    private final boolean allowNullValue;

    public CacheAnnotationParser() {
        this.allowNullValue = false;
    }

    public CacheAnnotationParser(boolean allowNullValue) {
        this.allowNullValue = allowNullValue;
    }

    /**
     * 解析方法缓存操作信息
     *
     * @param method 缓存操作方法
     */
    public Collection<AbsCacheOperation<? extends Annotation>> parseAnnotations(Method method, Class<?> targetClass) {
        Set<Annotation> annotations
                = AnnotatedElementUtils.getAllMergedAnnotations(method, CACHE_OPERATION_ANNOTATIONS);
        return this.parseAnnotations(targetClass, method, annotations);
    }

    public Collection<AbsCacheOperation<? extends Annotation>> parseAnnotations(Class<?> targetType,
                                                                                Method method,
                                                                                Set<? extends Annotation> annotations) {
        if (CollectionUtils.isEmpty(annotations)) {
            return Collections.emptyList();
        }
        CacheAction                                         cacheAction
                                                                       = AnnotatedElementUtils.findMergedAnnotation(targetType, CacheAction.class);
        Collection<AbsCacheOperation<? extends Annotation>> operations = new ArrayList<>(4);
        annotations.stream().filter(CacheEvict.class::isInstance)
                   .forEach(e -> operations.add(new CacheEvictOperation(method, (CacheEvict) e, cacheAction)));
        annotations.stream().filter(CachePut.class::isInstance)
                   .forEach(e -> operations.add(new CachePutOperation(method, allowNullValue, (CachePut) e, cacheAction)));
        annotations.stream().filter(CacheResult.class::isInstance)
                   .forEach(e -> operations.add(new CacheResultOperation(method, allowNullValue, (CacheResult) e, cacheAction)));
        return operations;
    }
}
