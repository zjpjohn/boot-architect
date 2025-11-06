package com.cloud.arch.cache.metrics;

import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.core.Ticker;
import io.micrometer.core.instrument.MeterRegistry;

public class CacheStatsManager implements StatsManager {

    private final MeterRegistry meterRegistry;

    public CacheStatsManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Ticker timeTicker() {
        return Ticker.systemTicker();
    }

    @Override
    public StatsCounter statsCounter(Cache cache) {
        MicroMeterStatsCounter statsCounter = new MicroMeterStatsCounter(meterRegistry, cache.getName());
        statsCounter.registerSizeMetric(cache);
        return statsCounter;
    }

}

