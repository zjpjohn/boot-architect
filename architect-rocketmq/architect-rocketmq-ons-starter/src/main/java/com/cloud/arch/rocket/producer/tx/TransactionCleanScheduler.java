package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.utils.RocketOnsConstants;
import com.cloud.arch.rocket.transaction.TransactionCleanHandler;
import org.springframework.scheduling.annotation.Scheduled;

public class TransactionCleanScheduler {

    public static final String TRANSACTION_GARBAGE_CRON = "${spring.cloud.ons.producer.cleanCron:0 0 0 1/7 * ?}";

    private final TransactionCleanHandler garbageHandler;
    private final OnsQueueProperties      queueProperties;

    public TransactionCleanScheduler(TransactionCleanHandler garbageHandler, OnsQueueProperties queueProperties) {
        this.garbageHandler  = garbageHandler;
        this.queueProperties = queueProperties;
    }

    @Scheduled(cron = TRANSACTION_GARBAGE_CRON)
    public void garbage() {
        Integer interval = this.queueProperties.getProducer().getCleanInterval();
        if (interval <= 0) {
            interval = RocketOnsConstants.DEFAULT_INTERVAL;
        }
        garbageHandler.handle(interval);
    }

}
