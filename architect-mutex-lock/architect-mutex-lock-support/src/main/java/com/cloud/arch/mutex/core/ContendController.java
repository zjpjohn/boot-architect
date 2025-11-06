package com.cloud.arch.mutex.core;

public interface ContendController extends AutoCloseable {

    MutexContender getContender();

    MutexState getMutexState();

    default void prepareMutex() {
    }

    default boolean hasOwner() {
        return getMutexState().getAfter() != MutexOwner.NONE;
    }

    default boolean isOwner() {
        return getMutexState().isOwner(this.getContender().getContenderId());
    }

    boolean isRunning();

    void start();

    void stop();

}
