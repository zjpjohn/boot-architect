package com.cloud.arch.cache.core;

public interface Ticker {

    long read();

    static Ticker systemTicker() {
        return SystemTicker.INSTANCE;
    }

    static Ticker disableTicker() {
        return DisabledTicker.INSTANCE;
    }

}

enum SystemTicker implements Ticker {
    INSTANCE;

    @Override
    public long read() {
        return System.nanoTime();
    }
}

enum DisabledTicker implements Ticker {
    INSTANCE;

    @Override
    public long read() {
        return 0L;
    }
}
