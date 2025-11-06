package com.cloud.arch.support.core;

import com.cloud.arch.annotations.OperateLog;
import com.cloud.arch.core.IFunctionFactory;
import com.cloud.arch.core.IOperatorFunction;
import com.cloud.arch.support.spel.OperationExpressionEvaluator;
import com.google.common.collect.Maps;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

public class LogOperateContextFactory implements BeanFactoryAware {

    private final OperationExpressionEvaluator                 evaluator      = new OperationExpressionEvaluator();
    private final Map<AnnotatedElementKey, AnnotationMetadata> cachedMetadata = Maps.newConcurrentMap();
    private final IOperatorFunction                            operatorFunction;
    private final IFunctionFactory                             functionFactory;
    private       BeanFactory                                  beanFactory;

    public LogOperateContextFactory(IOperatorFunction operatorFunction, IFunctionFactory functionFactory) {
        this.operatorFunction = operatorFunction;
        this.functionFactory  = functionFactory;
    }

    public LogOperateContext create(MethodInvocation invocation) {
        Object              target      = invocation.getThis();
        Class<?>            targetClass = AopProxyUtils.ultimateTargetClass(target);
        Method              method      = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        AnnotatedElementKey elementKey  = new AnnotatedElementKey(method, targetClass);
        AnnotationMetadata  metadata    = cachedMetadata.get(elementKey);
        if (metadata == null) {
            metadata
                    = new AnnotationMetadata(Objects.requireNonNull(AnnotationUtils.getAnnotation(method, OperateLog.class)));
            cachedMetadata.put(elementKey, metadata);
        }
        return new LogOperateContext(method, elementKey, invocation, metadata, operatorFunction, functionFactory, beanFactory, evaluator);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
