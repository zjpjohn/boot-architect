package com.cloud.arch.event.subscribe;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL 表达式解析器，缓存已编译的 {@link Expression} 对象，避免每次求值时重复解析和编译。
 */
public final class SpElExpressionParser {

    private static final SpelExpressionParser    PARSER = new SpelExpressionParser();
    private static final Map<String, Expression> CACHE  = new ConcurrentHashMap<>();

    private SpElExpressionParser() {
    }

    /**
     * 从缓存获取或编译 SpEL 表达式。
     */
    public static Expression get(String spel) {
        return CACHE.computeIfAbsent(spel, PARSER::parseExpression);
    }

    /**
     * 对目标对象求值表达式，返回指定类型。
     */
    public static <T> T evaluate(String spel, Object target, Class<T> type) {
        StandardEvaluationContext context = new StandardEvaluationContext(target);
        return get(spel).getValue(context, type);
    }
}
