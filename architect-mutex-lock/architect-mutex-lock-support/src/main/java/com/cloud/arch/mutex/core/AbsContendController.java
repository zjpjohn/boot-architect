package com.cloud.arch.mutex.core;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
public abstract class AbsContendController implements ContendController {

    protected final MutexContender contender;
    private final Executor       executor;

    private volatile MutexState mutexState = MutexState.NONE;
    private volatile boolean    running    = false;

    public AbsContendController(MutexContender contender, Executor executor) {
        this.contender = contender;
        this.executor  = executor;
    }

    @Override
    public MutexContender getContender() {
        return this.contender;
    }

    @Override
    public MutexState getMutexState() {
        return this.mutexState;
    }

    @Override
    public synchronized void start() {
        if (this.isRunning()) {
            return;
        }
        this.running    = true;
        this.mutexState = MutexState.NONE;
        this.startContend();
    }

    @Override
    public synchronized void stop() {
        if (!this.isRunning()) {
            return;
        }
        this.running = false;
        this.stopContend();
    }

    @Override
    public void close() throws Exception {
        this.stop();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    protected CompletableFuture<Void> notifyOwner(MutexOwner newOwner) {
        return CompletableFuture.runAsync(() -> {
            try {
                final MutexState newState = new MutexState(this.mutexState.getAfter(), newOwner);
                this.mutexState = newState;
                this.contender.contendNotify(newState);
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        }, executor);
    }

    protected abstract void startContend();

    protected abstract void stopContend();
}
