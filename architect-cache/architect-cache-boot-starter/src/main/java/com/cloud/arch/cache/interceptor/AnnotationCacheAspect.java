package com.cloud.arch.cache.interceptor;

import com.cloud.arch.cache.interceptor.context.CacheContextContainerFactory;
import com.cloud.arch.cache.support.CacheErrorHandler;
import com.cloud.arch.cache.support.CacheEvictManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class AnnotationCacheAspect extends CacheAspectSupport {


    public AnnotationCacheAspect(CacheEvictManager cacheEvictManager,
                                 CacheErrorHandler errorHandler,
                                 CacheContextContainerFactory operationContextsFactory) {
        super(cacheEvictManager, errorHandler, operationContextsFactory);
    }

    @Pointcut(
            "@annotation(com.cloud.arch.cache.annotations.CacheResult)"
            + "||@annotation(com.cloud.arch.cache.annotations.CachePut)"
            + "||@annotation(com.cloud.arch.cache.annotations.CacheEvict)")
    public void cacheActionPointcut() {
    }

    @Around("cacheActionPointcut()")
    public Object cacheOperation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method          method    = signature.getMethod();
        CacheOperationInvoker invoker = () -> {
            try {
                return joinPoint.proceed(joinPoint.getArgs());
            } catch (Throwable e) {
                throw new CacheOperationInvoker.ThrowableWrapper(e);
            }
        };
        try {
            return execute(invoker, joinPoint.getTarget(), method, joinPoint.getArgs());
        } catch (CacheOperationInvoker.ThrowableWrapper e) {
            throw new RuntimeException(e.getOriginal());
        }
    }

}
