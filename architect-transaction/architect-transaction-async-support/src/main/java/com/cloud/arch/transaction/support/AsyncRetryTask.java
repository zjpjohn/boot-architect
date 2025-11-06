package com.cloud.arch.transaction.support;

import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.google.common.primitives.Ints;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public record AsyncRetryTask(AsyncTxEvent event) implements Delayed {

    private Long timestamp() {
        LocalDateTime nextTime = this.event.getNextTime();
        return nextTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.timestamp() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Ints.saturatedCast(this.timestamp().compareTo(((AsyncRetryTask) o).timestamp()));
    }

}
