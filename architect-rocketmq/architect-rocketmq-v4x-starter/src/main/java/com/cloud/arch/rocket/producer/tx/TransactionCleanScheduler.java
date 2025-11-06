package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.transaction.TransactionCleanHandler;
import com.cloud.arch.rocket.utils.RocketmqConstants;
import org.springframework.scheduling.annotation.Scheduled;

public class TransactionCleanScheduler {

    public static final String TRANSACTION_GARBAGE_CRON = "${spring.cloud.rocketmq.producer.cleanCron:0 0 0 1/7 * ?}";

    private final TransactionCleanHandler garbageHandler;
    private final RocketmqProperties      properties;

    public TransactionCleanScheduler(TransactionCleanHandler garbageHandler, RocketmqProperties properties) {
        this.garbageHandler = garbageHandler;
        this.properties     = properties;
    }

    @Scheduled(cron = TRANSACTION_GARBAGE_CRON)
    public void garbage() {
        Integer interval = this.properties.getProducer().getCleanInterval();
        if (interval <= 0) {
            interval = RocketmqConstants.DEFAULT_INTERVAL;
        }
        garbageHandler.handle(interval);
    }
}
