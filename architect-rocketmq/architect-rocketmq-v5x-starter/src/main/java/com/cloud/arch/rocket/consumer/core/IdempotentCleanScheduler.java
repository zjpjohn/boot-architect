package com.cloud.arch.rocket.consumer.core;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.idempotent.IdempotentCleanHandler;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import org.springframework.scheduling.annotation.Scheduled;

public class IdempotentCleanScheduler {

    public static final String IDEMPOTENT_GARBAGE_CRON = "${spring.cloud.rocketmq.consumer.cleanCron:0 0 0 1/7 * ?}";

    private final IdempotentCleanHandler cleanHandler;
    private final RocketmqProperties     properties;

    public IdempotentCleanScheduler(IdempotentCleanHandler cleanHandler, RocketmqProperties properties) {
        this.cleanHandler = cleanHandler;
        this.properties   = properties;
    }

    @Scheduled(cron = IDEMPOTENT_GARBAGE_CRON)
    public void garbage() {
        Integer interval = properties.getConsumer().getCleanInterval();
        if (interval <= 0) {
            interval = RocketmqUtils.DEFAULT_INTERVAL;
        }
        cleanHandler.handle(interval);
    }

}
