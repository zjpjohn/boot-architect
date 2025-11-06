package com.cloud.arch.cache.interceptor;

import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.expression.CacheOperationExpressionEvaluator;
import com.cloud.arch.cache.interceptor.context.CacheContextContainer;
import com.cloud.arch.cache.interceptor.context.CacheContextContainerFactory;
import com.cloud.arch.cache.interceptor.context.OperationContext;
import com.cloud.arch.cache.interceptor.operation.CacheEvictOperation;
import com.cloud.arch.cache.interceptor.operation.CachePutOperation;
import com.cloud.arch.cache.interceptor.operation.CacheResultOperation;
import com.cloud.arch.cache.support.CacheErrorHandler;
import com.cloud.arch.cache.support.CacheEvictEvent;
import com.cloud.arch.cache.support.CacheEvictManager;
import com.cloud.arch.cache.support.CacheEvictPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.cloud.arch.cache.expression.CacheOperationExpressionEvaluator.NO_RESULT;


@Slf4j
public class CacheAspectSupport extends AbstractCacheInvoker implements DisposableBean {

    private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

    private final CacheContextContainerFactory operationContextsFactory;

    public CacheAspectSupport(CacheEvictManager cacheEvictManager,
                              CacheErrorHandler errorHandler,
                              CacheContextContainerFactory operationContextsFactory) {
        super(cacheEvictManager, errorHandler);
        this.operationContextsFactory = operationContextsFactory;
    }

    protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
        Class<?>              targetClass = getTargetClass(target);
        CacheContextContainer contexts    = operationContextsFactory.create(method, args, target, targetClass);
        if (contexts != null) {
            return execute(invoker, method, contexts);
        }
        return invoker.invoke();
    }

    private Object execute(CacheOperationInvoker invoker, Method method, CacheContextContainer contexts) {
        // 方法调用前置清除缓存操作,前置@CacheEvict处理
        precessCacheEvict(contexts.get(CacheEvictOperation.class), NO_RESULT, true);
        // 方法调用缓存数据操作,@CacheResult缓存处理
        Object cacheValue = processCacheResult(contexts.get(CacheResultOperation.class), () -> unwrapReturnValue(invoker.invoke()));
        // 方法调用更新缓存数据,@CachePut处理
        processCachePut(contexts.get(CachePutOperation.class), cacheValue);
        // 方法调用后置缓存清除操作,后置@CacheEvict处理
        precessCacheEvict(contexts.get(CacheEvictOperation.class), NO_RESULT, false);
        // 包装还原返回数据
        return wrapCacheValue(method, cacheValue);
    }

    private void processCachePut(Collection<OperationContext> contexts, Object result) {
        contexts.stream()
                .filter(v -> v.isConditionPassing(result, evaluator) && v.canCacheOrPut(result, evaluator))
                .findFirst()
                .ifPresent(v -> performCachePut(v, v.generateKey(result, evaluator), result));
    }

    private void performCachePut(OperationContext context, Object key, Object result) {
        Collection<Cache> caches = context.caches();
        for (Cache cache : caches) {
            doCachePut(cache, key, result);
        }
    }

    private Object wrapCacheValue(Method method, @Nullable Object cacheValue) {
        if (method.getReturnType() == Optional.class && (cacheValue == null
                || cacheValue.getClass() != Optional.class)) {
            return Optional.ofNullable(cacheValue);
        }
        return cacheValue;
    }

    private Object unwrapReturnValue(Object returnValue) {
        return ObjectUtils.unwrapOptional(returnValue);
    }

    /**
     * 执行缓存操作
     *
     * @param contexts 缓存数据上线文
     * @param callable 方法执行回调
     */
    private Object processCacheResult(Collection<OperationContext> contexts, Callable<?> callable) {
        OperationContext context = contexts.stream()
                                           .filter(v -> v.isConditionPassing(NO_RESULT, evaluator)
                                                   && v.canCacheOrPut(NO_RESULT, evaluator))
                                           .findFirst()
                                           .orElse(null);
        if (context == null) {
            return noneCacheCall(callable);
        }
        return findCacheResult(context, callable, context.generateKey(NO_RESULT, evaluator));
    }

    private Object noneCacheCall(Callable<?> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object findCacheResult(OperationContext context, Callable<?> callable, Object key) {
        Iterator<Cache> iterator = context.caches().iterator();
        Cache           cache    = iterator.next();
        Object          value    = cache.get(key, callable);
        while (iterator.hasNext()) {
            cache = iterator.next();
            cache.putIfAbsent(key, value);
        }
        return value;
    }

    private void precessCacheEvict(Collection<OperationContext> contexts, Object result, boolean beforeInvoke) {
        for (OperationContext context : contexts) {
            CacheEvictOperation operation = (CacheEvictOperation) context.getOperation();
            if (operation.isBeforeInvocation() == beforeInvoke && context.isConditionPassing(result, evaluator)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    /**
     * 执行缓存清除操作 注：前置清除缓存操作不支持延迟双删
     */
    private void performCacheEvict(OperationContext context, CacheEvictOperation operation, Object result) {
        Object      key          = context.generateKey(result, evaluator);
        boolean     allEntries   = operation.isAllEntries();
        boolean     beforeInvoke = operation.isBeforeInvocation();
        Set<String> cacheNames   = operation.getCacheNames();
        CacheEvictEvent[] evictEvents = cacheNames.stream()
                                                  .map(name -> new CacheEvictEvent(name, key, !beforeInvoke, allEntries))
                                                  .toArray(CacheEvictEvent[]::new);
        // 后置淘汰缓存，此时需要考虑缓存方法是否在事务中
        if (!beforeInvoke) {
            CacheEvictPublisher.publish(evictEvents);
            return;
        }
        // 前置淘汰缓存，直接淘汰缓存
        for (CacheEvictEvent evictEvent : evictEvents) {
            cacheEvictManager.cacheEvict(evictEvent);
        }
    }

    private Class<?> getTargetClass(Object target) {
        return AopProxyUtils.ultimateTargetClass(target);
    }

    @Override
    public void destroy() throws Exception {
        evaluator.clear();
    }

}
