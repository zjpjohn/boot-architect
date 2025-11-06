package com.cloud.arch.mutex.lock;

import com.cloud.arch.mutex.core.*;
import com.google.common.base.Strings;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

public class MutexLock extends AbsMutexContender implements Lock {

    private final    ContendController controller;
    private volatile Thread            owner;

    private static final AtomicReferenceFieldUpdater<MutexLock, Thread> OWNER
            = AtomicReferenceFieldUpdater.newUpdater(MutexLock.class, Thread.class, "owner");

    public MutexLock(String mutex, ContendMutexProps props, ContendControllerFactory controllerFactory) {
        super(mutex);
        this.controller = controllerFactory.createContendController(this, props);
        this.controller.prepareMutex();
    }

    @Override
    public void close() throws Exception {
        this.controller.stop();
    }

    @Override
    public void acquire() {
        if (OWNER.compareAndSet(this, null, Thread.currentThread())) {
            controller.start();
            LockSupport.park(this);
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    @Override
    public void acquire(Duration timeout) throws TimeoutException {
        if (OWNER.compareAndSet(this, null, Thread.currentThread())) {
            controller.start();
            LockSupport.parkNanos(this, timeout.toNanos());
            if (!controller.isOwner()) {
                throw new TimeoutException(Strings.lenientFormat("Could not acquire [%s]@mutex:[%s] within timeout of %sms", this.getContenderId(), this.getMutex(), timeout.toMillis()));
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    /**
     * 获取到竞争资源
     */
    @Override
    public void onAcquired(MutexState state) {
        LockSupport.unpark(OWNER.get(this));
    }

}
