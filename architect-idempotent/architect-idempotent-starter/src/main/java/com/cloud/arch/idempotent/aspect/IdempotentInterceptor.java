package com.cloud.arch.idempotent.aspect;

import com.cloud.arch.idempotent.support.IdempotentInfo;
import com.cloud.arch.idempotent.support.IdempotentManager;
import com.cloud.arch.idempotent.support.IdempotentMetaContainer;
import com.cloud.arch.idempotent.support.IdempotentRootObject;
import com.cloud.arch.web.error.ApiBizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public record IdempotentInterceptor(IdempotentManager manager, IdempotentMetaContainer idempotentContainer) {

    @Pointcut("@annotation(com.cloud.arch.idempotent.annotation.Idempotent)")
    public void idempotentPointcut() {
    }

    @Around("idempotentPointcut()")
    public Object invoke(ProceedingJoinPoint joinPoint) {
        IdempotentRootObject rootObject = IdempotentRootObject.of(joinPoint);
        IdempotentInfo       idempotent = idempotentContainer.getIdempotent(rootObject);
        Object               result     = null;
        Throwable            throwable  = null;
        try {
            boolean tryAcquire = manager.tryAcquire(idempotent);
            if (!tryAcquire) {
                return manager.acquireFail(idempotent);
            }
            result = joinPoint.proceed();
        } catch (Throwable error) {
            throwable = error;
            if (error instanceof ApiBizException exception) {
                throw exception;
            }
            throw new RuntimeException(error.getMessage(), error);
        } finally {
            manager.completed(idempotent, throwable);
        }
        return result;
    }

}
