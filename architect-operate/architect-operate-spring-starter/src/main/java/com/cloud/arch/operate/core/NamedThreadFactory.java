package com.cloud.arch.operate.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNum = new AtomicInteger(1);

    private final String      prefix;
    private final ThreadGroup group;
    private final boolean     daemon;

    public NamedThreadFactory(String prefix, boolean daemon) {
        this.prefix = prefix;
        this.daemon = daemon;
        this.group  = Thread.currentThread().getThreadGroup();
    }

    /**
     * Constructs a new {@code Thread}.  Implementations may also initialize
     * priority, name, daemon status, {@code ThreadGroup}, etc.
     *
     * @param runnable a runnable to be executed by new thread instance
     * @return constructed thread, or {@code null} if the request to
     * create a thread is rejected
     */
    @Override
    public Thread newThread(Runnable runnable) {
        String name   = prefix + threadNum.getAndIncrement();
        Thread thread = new Thread(group, runnable, name, 0);
        thread.setDaemon(daemon);
        return thread;
    }
}
