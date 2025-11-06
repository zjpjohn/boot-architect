package com.cloud.arch.event.subscribe;

import com.cloud.arch.event.commons.ApplicationContextHolder;
import com.cloud.arch.event.core.subscribe.SubscribeEventMetadata;
import com.cloud.arch.event.core.subscribe.SubscribeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Optional;

@Slf4j
public class EventSubScribeHandler implements SubscribeHandler {

    private static final String           EMPTY_SHARDING   = "";
    private final        ExpressionParser expressionParser = new SpelExpressionParser();

    private final IdempotentChecker idempotentChecker;

    public EventSubScribeHandler(IdempotentChecker idempotentChecker) {
        this.idempotentChecker = idempotentChecker;
    }

    /**
     * 领域事件处理
     *
     * @param eventKey 领域事件key
     * @param event    领域事件
     */
    @Override
    public void handle(String eventKey, Object event, SubscribeEventMetadata metadata) {
        // 获取分库分表分片键的值
        EventIdempotent idempotent = this.idempotent(eventKey, metadata, event);
        Throwable       throwable  = null;
        try {
            if (idempotentChecker.isProcessed(idempotent)) {
                return;
            }
            ApplicationContextHolder.publishEvent(event);
        } catch (Exception error) {
            throwable = error;
            throw error;
        } finally {
            idempotentChecker.markProcessed(idempotent, throwable);
        }
    }

    /**
     * 创建幂等信息
     *
     * @param eventKey 事件key
     * @param metadata 事件元数据
     * @param event    事件内容
     */
    public EventIdempotent idempotent(String eventKey, SubscribeEventMetadata metadata, Object event) {
        EventIdempotent idempotent = new EventIdempotent();
        idempotent.setName(metadata.getName());
        idempotent.setFilter(metadata.getFilter());
        idempotent.setEventKey(this.getEventKey(eventKey, metadata.getKey(), event));
        idempotent.setShardKey(this.getShardingKey(event, metadata.getSharding()));
        return idempotent;
    }

    /**
     * 从事件对象中获取分库分表分片键
     *
     * @param event      事件对象
     * @param shardField 分库分表字段名称
     */
    private String getShardingKey(Object event, String shardField) {
        if (StringUtils.isBlank(shardField)) {
            return EMPTY_SHARDING;
        }
        try {
            StandardEvaluationContext context    = new StandardEvaluationContext(event);
            Object                    shardValue = expressionParser.parseExpression(shardField).getValue(context);
            return Optional.ofNullable(shardValue).map(Object::toString).orElse("");
        } catch (Exception error) {
            log.warn(error.getMessage(), error);
        }
        return EMPTY_SHARDING;
    }

    /**
     * 获取事件幂等key
     *
     * @param eventKey 事件key
     * @param keySpel  幂等表达式
     * @param event    事件内容
     */
    private String getEventKey(String eventKey, String keySpel, Object event) {
        if (StringUtils.isBlank(keySpel)) {
            return eventKey;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext(event);
            Object                    key     = expressionParser.parseExpression(keySpel).getValue(context);
            return Optional.ofNullable(key).map(Object::toString).filter(StringUtils::isNotBlank).orElse(eventKey);
        } catch (Exception error) {
            log.warn(error.getMessage(), error);
        }
        return eventKey;
    }

}
