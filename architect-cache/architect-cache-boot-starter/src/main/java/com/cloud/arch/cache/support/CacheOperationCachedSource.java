package com.cloud.arch.cache.support;

import com.cloud.arch.cache.config.CloudCacheProperties;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.MethodClassKey;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class CacheOperationCachedSource implements CacheOperationSource, DisposableBean {

    private static final Collection<AbsCacheOperation<? extends Annotation>>              NULL_CACHING_ATTRIBUTE
                                                                                                         = Collections.emptyList();
    private final        Map<Object, Collection<AbsCacheOperation<? extends Annotation>>> attributeCache
                                                                                                         = new ConcurrentHashMap<>(1024);

    private final CacheAnnotationParser annotationParser;
    private final boolean               publicMethodsOnly;
    private final CacheResolver         cacheResolver;

    public CacheOperationCachedSource(CloudCacheProperties properties, CacheResolver cacheResolver) {
        this.publicMethodsOnly = properties.isOnlyPublic();
        this.cacheResolver     = cacheResolver;
        this.annotationParser  = new CacheAnnotationParser(properties.isAllowNullValue());
    }

    @Override
    public void destroy() throws Exception {
        attributeCache.clear();
    }

    /**
     * 获取方法上的缓存操作集合
     *
     * @param method     缓存操作方法
     * @param targetType 目标执行类
     */
    @Override
    public Collection<AbsCacheOperation<? extends Annotation>> getCacheOperations(Class<?> targetType, Method method) {
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }
        Object cacheKey = cacheKey(method, targetType);
        return Optional.ofNullable(this.attributeCache.get(cacheKey)).filter(value -> value != NULL_CACHING_ATTRIBUTE)
                       .orElse(null);
    }

    @Override
    public void cacheOperations(final Class<?> targetType,
                                final Method method,
                                final Set<? extends Annotation> annotations) {
        if (CollectionUtils.isEmpty(annotations)) {
            return;
        }
        Collection<AbsCacheOperation<? extends Annotation>> operations
                = annotationParser.parseAnnotations(targetType, method, annotations);
        if (CollectionUtils.isEmpty(operations)) {
            operations = NULL_CACHING_ATTRIBUTE;
        }
        Object cacheKey = cacheKey(method, targetType);
        this.attributeCache.put(cacheKey, operations);
    }


    @Override
    public void cacheBuild() {
        if (!CollectionUtils.isEmpty(attributeCache)) {
            attributeCache.values().stream().flatMap(Collection::stream).filter(AbsCacheOperation::canBuildCache)
                          .forEach(cacheResolver::resolveCache);
        }
    }

    protected Object cacheKey(Method method, Class<?> targetClass) {
        return new MethodClassKey(method, targetClass);
    }

}
