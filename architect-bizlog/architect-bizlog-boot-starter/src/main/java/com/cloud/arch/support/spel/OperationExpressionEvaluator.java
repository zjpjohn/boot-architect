package com.cloud.arch.support.spel;

import com.cloud.arch.support.core.LogOperateContext;
import com.cloud.arch.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class OperationExpressionEvaluator extends CachedExpressionEvaluator {

    private static final String                         NULL_VALUE_STR     = "null";
    private static final String                         SPEL_JSON_FUNCTION = "toJson";
    private final        Map<ExpressionKey, Expression> expressionCache    = new ConcurrentHashMap<>(64);


    /**
     * SpEl表达式计算
     *
     * @param expression 表达式
     * @param methodKey  缓存key
     * @param context    表达式上下文
     */
    public String getExpression(String expression, AnnotatedElementKey methodKey, EvaluationContext context) {
        Object value = getExpression(expressionCache, methodKey, expression).getValue(context, Object.class);
        return Optional.ofNullable(value).filter(v -> !NULL_VALUE_STR.equals(v)).map(Object::toString).orElse("");
    }

    /**
     * 创建SpEl表达式上下文
     *
     * @param context     当前日志操作上下文
     * @param beanFactory BeanFactory容器
     */
    public EvaluationContext createContext(LogOperateContext context, BeanFactory beanFactory) {
        try {
            LogEvaluatorContext evaluatorContext = new LogEvaluatorContext(context, getParameterNameDiscoverer());
            //注册json序列化方法
            evaluatorContext.registerFunction(SPEL_JSON_FUNCTION, JsonUtils.class.getDeclaredMethod("toJson", Object.class));
            if (beanFactory != null) {
                evaluatorContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
            }
            return evaluatorContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
