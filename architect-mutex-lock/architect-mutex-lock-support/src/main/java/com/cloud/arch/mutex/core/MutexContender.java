package com.cloud.arch.mutex.core;

public interface MutexContender {

    /**
     * 竞争资源标识
     */
    String getMutex();

    /**
     * 竞争者标识
     */
    String getContenderId();

    /**
     * 获取到竞争资源回调
     */
    default void onAcquired(MutexState state) {
    }

    /**
     * 释放竞争资源回调
     */
    default void onRelease(MutexState state) {
    }

    /**
     * 竞争资源通知回调
     */
    default void contendNotify(MutexState state) {
        if (!state.isChanged()) {
            return;
        }
        if (state.isAcquired(this.getContenderId())) {
            this.onAcquired(state);
        }
        if (state.isReleased(this.getContenderId())) {
            this.onRelease(state);
        }
    }
}
