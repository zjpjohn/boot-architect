package com.cloud.arch.transaction.interceptor;

import com.cloud.arch.transaction.annotation.TxAsync;
import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.cloud.arch.transaction.core.AsyncTxInvoker;
import com.cloud.arch.transaction.core.AsyncTxInvokers;
import com.cloud.arch.transaction.support.AsyncTxEventHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;

import java.lang.reflect.Method;

@Aspect
public class AsyncTransactionInterceptor {

    @Pointcut("@annotation(com.cloud.arch.transaction.annotation.TxAsync)")
    public void txAsyncPointcut() {
    }

    @Around("txAsyncPointcut()")
    public Object invoke(ProceedingJoinPoint joinPoint) {
        Object[]       arguments      = joinPoint.getArgs();
        String         asyncKey       = this.asyncKey(joinPoint);
        AsyncTxInvoker asyncTxInvoker = AsyncTxInvokers.get(asyncKey);
        if (asyncTxInvoker != null) {
            AsyncTxEvent asyncTxEvent = asyncTxInvoker.build(arguments);
            AsyncTxEventHolder.publish(asyncTxEvent);
            return null;
        }
        try {
            return joinPoint.proceed(arguments);
        } catch (Throwable error) {
            throw new RuntimeException(error.getMessage(), error);
        }
    }

    /**
     * 构建异步任务key
     */
    private String asyncKey(ProceedingJoinPoint joinPoint) {
        Object   target      = joinPoint.getTarget();
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        Method   method      = ((MethodSignature) joinPoint.getSignature()).getMethod();
        TxAsync  annotation  = method.getAnnotation(TxAsync.class);
        return AsyncTxInvoker.asyncKey(targetClass, method, annotation.name());
    }

}
