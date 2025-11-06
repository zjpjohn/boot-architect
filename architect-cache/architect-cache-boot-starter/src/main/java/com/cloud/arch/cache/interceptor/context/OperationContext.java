package com.cloud.arch.cache.interceptor.context;

import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.expression.CacheOperationExpressionEvaluator;
import com.cloud.arch.cache.interceptor.operation.AbsCacheOperation;
import com.cloud.arch.cache.interceptor.operation.CacheOperationMetadata;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

public class OperationContext implements CacheContext {

    private final CacheOperationMetadata metadata;
    private final Object[]               args;
    private final Object                 target;
    private final Collection<Cache>      caches;

    public OperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
        this.metadata = metadata;
        this.target   = target;
        this.args     = extractArgs(getMethod(), args);
        this.caches   = metadata.getCaches();
    }

    public boolean isConditionPassing(Object result, CacheOperationExpressionEvaluator evaluator) {
        return metadata.isConditionPassing(this, evaluator, result);
    }

    public boolean canCacheOrPut(Object result, CacheOperationExpressionEvaluator evaluator) {
        return metadata.canCacheOrPut(this, evaluator, result);
    }

    public Object generateKey(Object result, CacheOperationExpressionEvaluator evaluator) {
        return metadata.generateKey(this, evaluator, result);
    }

    @Override
    public AbsCacheOperation<? extends Annotation> getOperation() {
        return this.metadata.getOperation();
    }

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public Method getMethod() {
        return metadata.getMethod();
    }

    @Override
    public Object[] getArgs() {
        return this.args;
    }

    @Override
    public Collection<Cache> caches() {
        return this.caches;
    }

    private Object[] extractArgs(Method method, Object[] args) {
        if (!method.isVarArgs()) {
            return args;
        }
        //多个参数可变参数一定在最后面
        Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
        //组合参数集合
        Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
        System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
        System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
        return combinedArgs;
    }

}
