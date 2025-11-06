package com.cloud.arch.mutex;

import com.cloud.arch.mutex.core.AbsContendController;
import com.cloud.arch.mutex.core.ContendPeriod;
import com.cloud.arch.mutex.core.MutexContender;
import com.cloud.arch.mutex.core.MutexOwner;
import com.cloud.arch.mutex.utils.Threads;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.*;

@Slf4j
public class JdbcContendController extends AbsContendController {

    private final Duration              initialDelay;
    private final Duration              ttl;
    private final Duration              transition;
    private final ContendPeriod         contendPeriod;
    private final IMutexOwnerRepository mutexOwnerRepository;

    private          ScheduledExecutorService contendScheduler;
    private volatile ScheduledFuture<?>       contendFuture;

    public JdbcContendController(MutexContender contender,
                                 Executor executor,
                                 IMutexOwnerRepository mutexOwnerRepository,
                                 Duration initialDelay,
                                 Duration ttl,
                                 Duration transition) {
        super(contender, executor);
        this.initialDelay         = initialDelay;
        this.ttl                  = ttl;
        this.transition           = transition;
        this.contendPeriod        = new ContendPeriod(this.getContender().getContenderId());
        this.mutexOwnerRepository = mutexOwnerRepository;
    }

    @Override
    public void prepareMutex() {
        //初始资源防止资源不存在
        this.mutexOwnerRepository.initMutex(this.getContender().getMutex());
    }

    @Override
    protected void startContend() {
        //竞争资源调度器
        String threadName
                = Strings.lenientFormat("jdbc_contender_%s_%s", getContender().getMutex(), getContender().getContenderId());
        contendScheduler
                = new ScheduledThreadPoolExecutor(1, Threads.threadFactory(threadName), new ThreadPoolExecutor.DiscardPolicy());
        //调度竞争资源
        this.nextSchedule(initialDelay.toMillis());
    }

    private void nextSchedule(long nextDelay) {
        this.contendFuture = this.contendScheduler.schedule(this::safeHandleContend, nextDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void stopContend() {
        if (this.contendFuture != null) {
            this.contendFuture.cancel(true);
        }
        if (contendScheduler != null) {
            this.contendScheduler.shutdown();
        }
        notifyOwner(MutexOwner.NONE);
        mutexOwnerRepository.release(getContender().getMutex(), getContender().getContenderId());
    }

    private void safeHandleContend() {
        try {
            final MutexOwner mutexOwner = this.contend();
            notifyOwner(mutexOwner);
            final long nextDelay = this.contendPeriod.ensureNextDelay(mutexOwner);
            this.nextSchedule(nextDelay);
        } catch (Exception error) {
            if (log.isErrorEnabled()) {
                log.error(error.getMessage(), error);
            }
            this.nextSchedule(ttl.toMillis());
        }
    }

    private MutexOwner contend() {
        return mutexOwnerRepository.acquireAndGetOwner(getContender().getMutex(), getContender().getContenderId(), ttl.toMillis(), transition.toMillis());
    }

}
