package com.cloud.arch.support.spel;

import com.cloud.arch.core.OperateContext;
import com.cloud.arch.support.core.LogOperateContext;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.util.Map;


public class LogEvaluatorContext extends MethodBasedEvaluationContext {


    public LogEvaluatorContext(LogOperateContext context, ParameterNameDiscoverer discoverer) {
        super(context.getTarget(), context.getMethod(), context.getParameters(), discoverer);
        if (context.getReturnValue() != null) {
            setVariable("_return", context.getReturnValue());
        }
        if (context.getResult() != null) {
            setVariable("_error", context.getResult().getMessage());
        }
        Map<String, Object> variables = OperateContext.getVariables();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            setVariable(entry.getKey(), entry.getValue());
        }
    }
}
