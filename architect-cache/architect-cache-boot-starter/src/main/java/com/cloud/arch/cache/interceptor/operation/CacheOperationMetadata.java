package com.cloud.arch.cache.interceptor.operation;

import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.expression.CacheOperationExpressionEvaluator;
import com.cloud.arch.cache.interceptor.context.OperationContext;
import com.cloud.arch.cache.support.CacheResolver;
import com.cloud.arch.cache.support.KeyGenerator;
import lombok.Getter;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

@Getter
public class CacheOperationMetadata {

    private final AbsCacheOperation<? extends Annotation> operation;
    private final Method                                  method;
    private final AnnotatedElementKey                     methodKey;
    private final KeyGenerator                            keyGenerator;
    private final CacheResolver                           cacheResolver;

    public CacheOperationMetadata(AbsCacheOperation<? extends Annotation> operation,
                                  Method method,
                                  Class<?> targetClass,
                                  KeyGenerator keyGenerator,
                                  CacheResolver cacheResolver) {
        this.operation     = operation;
        this.method        = BridgeMethodResolver.findBridgedMethod(method);
        this.keyGenerator  = keyGenerator;
        this.cacheResolver = cacheResolver;
        Method targetMethod = (Proxy.isProxyClass(targetClass) ?
                               AopUtils.getMostSpecificMethod(method, targetClass) :
                               this.method);
        this.methodKey = new AnnotatedElementKey(targetMethod, targetClass);
    }

    public Collection<Cache> getCaches() {
        return cacheResolver.resolveCache(operation);
    }

    private EvaluationContext createEvaluationContext(OperationContext context,
                                                      CacheOperationExpressionEvaluator evaluator,
                                                      Object result) {
        return evaluator.createEvaluationContext(method, context.getArgs(), context.getTarget(), result);
    }

    public boolean isConditionPassing(OperationContext context,
                                      CacheOperationExpressionEvaluator evaluator,
                                      Object result) {
        String condition = this.operation.getCondition();
        if (!StringUtils.hasText(condition)) {
            return Boolean.TRUE;
        }
        EvaluationContext evaluationContext = createEvaluationContext(context, evaluator, result);
        return evaluator.condition(condition, methodKey, evaluationContext);
    }

    public boolean canCacheOrPut(OperationContext context, CacheOperationExpressionEvaluator evaluator, Object result) {
        String unless = "";
        if (operation instanceof CacheResultOperation) {
            unless = ((CacheResultOperation) operation).getUnless();
        } else if (operation instanceof CachePutOperation) {
            unless = ((CachePutOperation) operation).getUnless();
        }
        if (!StringUtils.hasText(unless)) {
            return Boolean.TRUE;
        }
        EvaluationContext evaluationContext = createEvaluationContext(context, evaluator, result);
        return !evaluator.unless(unless, methodKey, evaluationContext);
    }

    public Object generateKey(OperationContext context, CacheOperationExpressionEvaluator evaluator, Object result) {
        String key = operation.getKey();
        if (!StringUtils.hasText(key)) {
            //没有Key SPEL表达式，使用KeyGenerator生成key
            return keyGenerator.generate(context.getTarget(), method, context.getArgs());
        }
        EvaluationContext evaluationContext = createEvaluationContext(context, evaluator, result);
        Object            cacheKey          = evaluator.key(key, methodKey, evaluationContext);
        if (cacheKey == null) {
            //如果SPEL表达式错误，抛出异常提示表达式错误
            throw new IllegalArgumentException(String.format("Cache key expression '%s' error, no cache key can be evaluated.", key));
        }
        return cacheKey;
    }

}
