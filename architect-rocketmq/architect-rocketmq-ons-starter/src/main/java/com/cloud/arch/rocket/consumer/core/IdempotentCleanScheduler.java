package com.cloud.arch.rocket.consumer.core;

import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.idempotent.IdempotentCleanHandler;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import org.springframework.scheduling.annotation.Scheduled;

import static com.cloud.arch.rocket.utils.RocketmqUtils.DEFAULT_CLEAN_CRON;

public class IdempotentCleanScheduler {

    public static final String IDEMPOTENT_GARBAGE_CRON = "${spring.cloud.ons.consumer.cleanCron:0 0 0 1/7 * ?}";

    private final IdempotentCleanHandler garbageHandler;
    private final OnsQueueProperties     queueProperties;

    public IdempotentCleanScheduler(IdempotentCleanHandler garbageHandler, OnsQueueProperties queueProperties) {
        this.garbageHandler  = garbageHandler;
        this.queueProperties = queueProperties;
    }

    @Scheduled(cron = DEFAULT_CLEAN_CRON)
    public void garbage() {
        Integer interval = queueProperties.getConsumer().getCleanInterval();
        interval = Math.max(interval, RocketmqUtils.DEFAULT_INTERVAL);
        garbageHandler.handle(interval);
    }

}
