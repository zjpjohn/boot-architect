package com.cloud.arch.cache.expression;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationException;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class CacheEvaluationContext extends MethodBasedEvaluationContext {

    private final Set<String> unavailableVariables = HashSet.newHashSet(1);

    public CacheEvaluationContext(Object rootObject,
                                  Method method,
                                  Object[] args,
                                  ParameterNameDiscoverer parameterNameDiscoverer) {
        super(rootObject, method, args, parameterNameDiscoverer);
    }

    public void addUnavailableVariable(String name) {
        this.unavailableVariables.add(name);
    }

    @Override
    public Object lookupVariable(String name) {
        if (this.unavailableVariables.contains(name)) {
            throw new EvaluationException("variable '" + name + "' is not available.");
        }
        return super.lookupVariable(name);
    }
}
