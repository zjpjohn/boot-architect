package com.cloud.arch.rocket.consumer.core;

import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.utils.RocketOnsConstants;
import com.cloud.arch.rocket.idempotent.IdempotentCleanHandler;
import org.springframework.scheduling.annotation.Scheduled;

public class IdempotentCleanScheduler {

    public static final String IDEMPOTENT_GARBAGE_CRON = "${spring.cloud.ons.consumer.cleanCron:0 0 0 1/7 * ?}";

    private final IdempotentCleanHandler garbageHandler;
    private final OnsQueueProperties     queueProperties;

    public IdempotentCleanScheduler(IdempotentCleanHandler garbageHandler, OnsQueueProperties queueProperties) {
        this.garbageHandler  = garbageHandler;
        this.queueProperties = queueProperties;
    }

    @Scheduled(cron = IDEMPOTENT_GARBAGE_CRON)
    public void garbage() {
        Integer interval = queueProperties.getConsumer().getCleanInterval();
        if (interval <= 0) {
            interval = RocketOnsConstants.DEFAULT_INTERVAL;
        }
        garbageHandler.handle(interval);
    }
}
