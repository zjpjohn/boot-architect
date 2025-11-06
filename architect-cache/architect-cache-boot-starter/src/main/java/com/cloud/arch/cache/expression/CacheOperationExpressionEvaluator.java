package com.cloud.arch.cache.expression;


import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

    public static final Object NO_RESULT          = new Object();
    public static final Object RESULT_UNAVAILABLE = new Object();
    public static final String RESULT_VARIABLE    = "result";

    private final Map<ExpressionKey, Expression> keyCache       = new ConcurrentHashMap<>(64);
    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);
    private final Map<ExpressionKey, Expression> unlessCache    = new ConcurrentHashMap<>(64);

    public EvaluationContext createEvaluationContext(Method method,
                                                     Object[] args,
                                                     Object target,
                                                     Object result) {
        Class<?>                  targetClass       = AopProxyUtils.ultimateTargetClass(target);
        CacheExpressionRootObject rootObject        = new CacheExpressionRootObject(method, args, target, targetClass);
        CacheEvaluationContext    evaluationContext = new CacheEvaluationContext(rootObject, getTargetMethod(targetClass, method), args, this.getParameterNameDiscoverer());
        if (RESULT_UNAVAILABLE == result) {
            evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
        } else if (NO_RESULT == result) {
            evaluationContext.setVariable(RESULT_VARIABLE, result);
        }
        return evaluationContext;
    }

    public Object key(String expression, AnnotatedElementKey elementKey, EvaluationContext context) {
        return getExpression(keyCache, elementKey, expression).getValue(context);
    }

    public Boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.conditionCache, methodKey, conditionExpression).getValue(evalContext, Boolean.class);
    }

    public Boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
        return getExpression(this.unlessCache, methodKey, unlessExpression).getValue(evalContext, Boolean.class);
    }

    public Method getTargetMethod(Class<?> targetClass, Method method) {
        if (Proxy.isProxyClass(targetClass)) {
            return AopUtils.getMostSpecificMethod(method, targetClass);
        }
        return method;
    }

    public void clear() {
        this.keyCache.clear();
        this.conditionCache.clear();
        this.unlessCache.clear();
    }
}
