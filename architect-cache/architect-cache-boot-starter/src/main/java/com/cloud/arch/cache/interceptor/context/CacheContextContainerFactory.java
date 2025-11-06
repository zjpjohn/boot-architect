package com.cloud.arch.cache.interceptor.context;

import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import com.cloud.arch.cache.interceptor.operation.CacheOperationKey;
import com.cloud.arch.cache.interceptor.operation.CacheOperationMetadata;
import com.cloud.arch.cache.support.CacheOperationSource;
import com.cloud.arch.cache.support.CacheResolver;
import com.cloud.arch.cache.support.KeyGenerator;
import com.cloud.arch.cache.support.SimpleCacheResolver;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CacheContextContainerFactory implements BeanFactoryAware, DisposableBean {

    private final Map<CacheOperationKey, CacheOperationMetadata> metadataCache = new ConcurrentHashMap<>(1024);

    private final KeyGenerator         keyGenerator;
    private final CacheResolver        cacheResolver;
    private final CacheOperationSource operationSource;
    private       BeanFactory          beanFactory;

    public CacheContextContainerFactory(KeyGenerator keyGenerator,
                                        CacheResolver cacheResolver,
                                        CacheManager cacheManager,
                                        CacheOperationSource operationSource) {
        this.keyGenerator    = keyGenerator;
        this.operationSource = operationSource;
        this.cacheResolver   = Optional.ofNullable(cacheResolver)
                                       .orElseGet(() -> new SimpleCacheResolver(cacheManager));
    }

    /**
     * 创建当前方法全部缓存操作上下文信息
     *
     * @param method      当前方法
     * @param args        方法参数
     * @param target      当前实例
     * @param targetClass 当前实例类型
     */
    public CacheContextContainer create(Method method, Object[] args, Object target, Class<?> targetClass) {
        Collection<AbsCacheOperation<? extends Annotation>> operations
                = operationSource.getCacheOperations(targetClass, method);
        if (CollectionUtils.isEmpty(operations)) {
            return null;
        }
        return new CacheContextContainer(operations, operation -> getOperationContext(operation, method, args, target, targetClass));
    }

    private OperationContext getOperationContext(AbsCacheOperation<? extends Annotation> operation,
                                                 Method method,
                                                 Object[] args,
                                                 Object target,
                                                 Class<?> targetClass) {
        CacheOperationMetadata metadata = getOperationMetadata(operation, method, targetClass);
        return new OperationContext(metadata, args, target);
    }

    private CacheOperationMetadata getOperationMetadata(AbsCacheOperation<? extends Annotation> operation,
                                                        Method method,
                                                        Class<?> targetClass) {
        CacheOperationKey operationKey = new CacheOperationKey(operation, method, targetClass);
        return this.metadataCache.computeIfAbsent(operationKey, key -> {
            KeyGenerator keyGenerator = this.keyGenerator;
            if (StringUtils.hasText(operation.getKeyGenerator())) {
                keyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
            }
            CacheResolver cacheResolver = this.cacheResolver;
            if (StringUtils.hasText(operation.getCacheResolver())) {
                cacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
            }
            return new CacheOperationMetadata(operation, method, targetClass, keyGenerator, cacheResolver);
        });

    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {
        metadataCache.clear();
    }

    protected <T> T getBean(String beanName, Class<T> expectedType) {
        if (this.beanFactory == null) {
            throw new IllegalStateException("BeanFactory must be set on cache aspect for "
                                            + expectedType.getSimpleName()
                                            + " retrieval");
        }
        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
    }

}
