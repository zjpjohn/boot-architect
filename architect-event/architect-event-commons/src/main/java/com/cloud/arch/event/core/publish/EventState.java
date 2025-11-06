package com.cloud.arch.event.core.publish;

import java.util.Arrays;

public enum EventState {
    INITIALIZED(0),
    SUCCEEDED(1),
    FAILED(2);

    private final Integer state;

    EventState(Integer state) {
        this.state = state;
    }

    public Integer getState() {
        return state;
    }

    public static EventState of(int value) {
        return Arrays.stream(values())
                .filter(e -> e.state == value)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("unknown state value:" + value));
    }
}
