package com.cloud.arch.idempotent.support;

import com.cloud.arch.web.error.ApiBizException;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class IdempotentParseEvaluator extends CachedExpressionEvaluator {

    private final Map<ExpressionKey, Expression> keyCache      = new ConcurrentHashMap<>(16);
    private final Map<ExpressionKey, Expression> shardingCache = new ConcurrentHashMap<>(16);

    public String key(String expression, AnnotatedElementKey elementKey, EvaluationContext context) {
        if (StringUtils.hasText(expression)) {
            return getExpression(keyCache, elementKey, expression).getValue(context, String.class);
        }
        throw new ApiBizException(HttpStatus.INTERNAL_SERVER_ERROR, 500, "幂等key配置错误");
    }

    public String sharding(String expression, AnnotatedElementKey elementKey, EvaluationContext context) {
        if (StringUtils.hasText(expression)) {
            return getExpression(shardingCache, elementKey, expression).getValue(context, String.class);
        }
        return "";
    }

    public EvaluationContext getContext(IdempotentRootObject root) {
        return new MethodBasedEvaluationContext(root, root.getTargetMethod(), root.args(), new DefaultParameterNameDiscoverer());
    }

}
