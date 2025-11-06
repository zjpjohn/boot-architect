package com.cloud.arch.support.core;

import com.cloud.arch.core.*;
import com.cloud.arch.support.spel.OperationExpressionEvaluator;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LogOperateContext {

    public static final Pattern FUNCTION_PATTERN    = Pattern.compile("\\{\\s*(\\w*)\\s*\\{(.*?)}}");
    public static final String  DOLLAR_CHAR_REGEX   = "\\$";
    public static final String  DOLLAR_CHAR_REPLACE = ".";

    private final Method                       method;
    private final AnnotatedElementKey          elementKey;
    private final MethodInvocation             invocation;
    private final AnnotationMetadata           metadata;
    private final IOperatorFunction            operatorFunction;
    private final IFunctionFactory             functionFactory;
    private final BeanFactory                  beanFactory;
    private final OperationExpressionEvaluator evaluator;
    private       ExecuteResultHolder          result;
    private       Object                       returnValue;
    //前置函数值集合
    private final Map<String, String>          beforeFunctionValues = Maps.newHashMap();
    //全部表达式值集合
    private final Map<String, String>          expressionValues     = Maps.newHashMap();

    public LogOperateContext(Method method,
                             AnnotatedElementKey elementKey,
                             MethodInvocation invocation,
                             AnnotationMetadata metadata,
                             IOperatorFunction operatorFunction,
                             IFunctionFactory functionFactory,
                             BeanFactory beanFactory,
                             OperationExpressionEvaluator evaluator) {
        this.method           = method;
        this.invocation       = invocation;
        this.metadata         = metadata;
        this.elementKey       = elementKey;
        this.operatorFunction = operatorFunction;
        this.functionFactory  = functionFactory;
        this.beanFactory      = beanFactory;
        this.evaluator        = evaluator;
        this.result           = new ExecuteResultHolder(true);
    }

    /**
     * 方法执行前计算
     */
    public void beforeExecute() {
        EvaluationContext   context      = evaluator.createContext(this, beanFactory);
        Map<String, String> beforeValues = Maps.newHashMap();
        List<String>        templates    = metadata.getSpelTemplates(true);
        templates.stream().filter(template -> template.contains("{")).forEach(template -> {
            Matcher matcher = FUNCTION_PATTERN.matcher(template);
            while (matcher.find()) {
                String expression = matcher.group(2);
                if (expression.contains("#_return") || expression.contains("#_error")) {
                    continue;
                }
                String functionName = matcher.group(1);
                if (functionFactory.isBefore(functionName)) {
                    String value = evaluator.getExpression(expression, elementKey, context);
                    beforeValues.put(functionName, functionFactory.apply(functionName, value));
                }
            }
        });
        this.beforeFunctionValues.putAll(beforeValues);
    }

    /**
     * 方法执行后计算
     */
    public void afterExecute() {
        List<String>        templates   = this.metadata.getSpelTemplates(result.isSuccess());
        EvaluationContext   context     = evaluator.createContext(this, beanFactory);
        Map<String, String> afterValues = Maps.newHashMap();
        for (String template : templates) {
            //纯字符串不包含SPEL表达式
            if (!template.contains("{")) {
                afterValues.put(template, template);
                continue;
            }
            /**
             * SPEL表达式格式{{#user.userName}} or {FUNCTION_NAME{#function_param}}
             */
            Matcher       matcher   = FUNCTION_PATTERN.matcher(template);
            StringBuilder parserStr = new StringBuilder();
            while (matcher.find()) {
                String functionName   = matcher.group(1);
                String functionResult = this.beforeFunctionValues.get(functionName);
                if (!StringUtils.hasText(functionResult)) {
                    String expression = matcher.group(2);
                    String value      = evaluator.getExpression(expression, elementKey, context);
                    functionResult = functionFactory.apply(functionName, value)
                                                    .replaceAll(DOLLAR_CHAR_REGEX, DOLLAR_CHAR_REPLACE);
                }
                matcher.appendReplacement(parserStr, Strings.nullToEmpty(functionResult));
            }
            matcher.appendTail(parserStr);
            afterValues.put(template, parserStr.toString());
        }
        this.expressionValues.putAll(afterValues);
    }

    /**
     * 获取spel表达式的值
     */
    private String getSpElValue(String key) {
        return Optional.ofNullable(this.expressionValues.get(key)).orElse("");
    }

    /**
     * 操作日志过滤器
     */
    public boolean conditional() {
        return !StringUtils.hasText(this.metadata.getCondition())
                || StringUtils.endsWithIgnoreCase(expressionValues.get(this.metadata.getCondition()), "true");
    }

    /**
     * 获取操作者operator
     */
    public Operator getOperator() {
        String operatorId = null;
        if (StringUtils.hasText(this.metadata.getOperator())) {
            operatorId = this.expressionValues.get(this.metadata.getOperator());
        }
        Operator operator = operatorFunction.operator(operatorId);
        if (!StringUtils.hasText(operator.operator())) {
            throw new IllegalArgumentException("Log operator must not be null.");
        }
        return operator;
    }

    /**
     * 操作日志
     */
    public LogRecord logRecord(String application) {
        if (conditional()) {
            Operator operator = this.getOperator();
            String   action   = this.result.isSuccess() ? this.metadata.getSuccess() : this.metadata.getFail();
            return LogRecord.builder()
                            .app(application)
                            .group(this.metadata.getGroup())
                            .bizNo(this.getSpElValue(this.metadata.getBizNo()))
                            .detail(this.getSpElValue(this.metadata.getDetail()))
                            .tenant(this.getSpElValue(this.metadata.getTenant()))
                            .operatorId(operator.operatorId())
                            .operator(operator.operator())
                            .action(this.getSpElValue(action))
                            .fail(this.result.isSuccess() ? 0 : 1)
                            .gmtCreate(LocalDateTime.now())
                            .build();
        }
        return null;
    }

    /**
     * 执行方法
     */
    public Object execute() throws Throwable {
        try {
            OperateContext.putEmptySpan();
            this.beforeExecute();
        } catch (Exception e) {
            log.warn("Log operation parse before execute error:", e);
        }
        try {
            this.returnValue = invocation.proceed();
        } catch (Throwable throwable) {
            this.result = new ExecuteResultHolder(false, throwable);
        }
        try {
            this.afterExecute();
        } catch (Exception e) {
            log.warn("Log operation after execute error:", e);
        } finally {
            OperateContext.clear();
        }
        if (result.getThrowable() != null) {
            throw result.getThrowable();
        }
        return this.returnValue;
    }

    public Object getTarget() {
        return invocation.getThis();
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return invocation.getArguments();
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public ExecuteResultHolder getResult() {
        return result;
    }

}
