package com.cloud.arch.idempotent.support;

import com.cloud.arch.idempotent.annotation.Idempotent;
import lombok.Getter;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Getter
public class IdempotentMeta {

    private final String   prefix;
    private final String   key;
    private final String   sharding;
    private final Long     expireTime;
    private final TimeUnit timeUnit;
    private final String   message;
    private final boolean  removeNow;

    public IdempotentMeta(Idempotent idempotent) {
        this.prefix     = idempotent.prefix();
        this.key        = idempotent.key();
        this.sharding   = idempotent.sharding();
        this.expireTime = idempotent.expireTime();
        this.timeUnit   = idempotent.timeUnit();
        this.message    = idempotent.message();
        this.removeNow  = idempotent.removeNow();
    }

    public IdempotentInfo getIdempotent(AnnotatedElementKey elementKey,
                                        EvaluationContext context,
                                        IdempotentParseEvaluator evaluator) {
        String idemKey = evaluator.key(key, elementKey, context);
        //自定义添加前缀
        if (StringUtils.hasText(prefix)) {
            idemKey = prefix + ":" + idemKey;
        }
        //默认添加前缀idm
        idemKey = "idm:" + idemKey;
        String   shardingKey = evaluator.sharding(sharding, elementKey, context);
        Duration duration    = Duration.ofMillis(timeUnit.toMillis(expireTime));
        return new IdempotentInfo(idemKey, shardingKey, duration, message, removeNow);
    }

}
