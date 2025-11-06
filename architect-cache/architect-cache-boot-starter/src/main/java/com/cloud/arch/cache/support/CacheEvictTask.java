package com.cloud.arch.cache.support;

import com.google.common.primitives.Ints;
import lombok.Getter;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Getter
public class CacheEvictTask implements Delayed {

    private final CacheEvictEvent event;
    private final Long            timestamp;

    public CacheEvictTask(CacheEvictEvent event, Long timestamp) {
        this.event     = event;
        this.timestamp = timestamp;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.timestamp - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Ints.saturatedCast(this.timestamp.compareTo(((CacheEvictTask) o).timestamp));
    }

}
