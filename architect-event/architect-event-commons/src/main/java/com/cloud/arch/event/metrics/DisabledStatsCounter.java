package com.cloud.arch.event.metrics;

public enum DisabledStatsCounter implements EventStatsCounter {

    INSTANCE;

    @Override
    public void recordPublishSuccess(long durationMs) {
    }

    @Override
    public void recordPublishFailure(long durationMs) {
    }

    @Override
    public void recordPublishFallbackSync() {
    }

    @Override
    public void recordConsumeSuccess(long durationMs) {
    }

    @Override
    public void recordConsumeFailure(long durationMs) {
    }

    @Override
    public void recordConsumeDuplicate(long durationMs) {
    }

}
