package com.cloud.arch.cache.metrics;


import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.core.Ticker;

public interface StatsManager {

    default Ticker timeTicker() {
        return Ticker.disableTicker();
    }

    default StatsCounter statsCounter(Cache cache) {
        return StatsCounter.disabledStatsCounter();
    }

    static StatsManager disabledManager() {
        return DisableStatsManager.INSTANCE;
    }

}

enum DisableStatsManager implements StatsManager {
    INSTANCE;
}
