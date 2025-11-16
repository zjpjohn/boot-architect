package com.cloud.arch.mutex;


import com.alibaba.fastjson2.JSON;
import com.cloud.arch.mutex.core.AbsContendController;
import com.cloud.arch.mutex.core.ContendPeriod;
import com.cloud.arch.mutex.core.MutexContender;
import com.cloud.arch.mutex.core.MutexOwner;
import com.cloud.arch.mutex.utils.Threads;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;

@Slf4j
public class RedisContendController extends AbsContendController {

    private static final String KEY_PREFIX     = "arch:mutex";
    private static final String ACQUIRE_SCRIPT = loadLua("mutex_acquire.lua");
    private static final String GUARD_SCRIPT   = loadLua("mutex_guard.lua");
    private static final String RELEASE_SCRIPT = loadLua("mutex_release.lua");

    private final RedissonClient redissonClient;
    private final String[]       keys;
    private final String         mutexChannel;
    private final String         contenderChannel;
    private final Duration       ttl;
    private final Duration       transition;
    private final ContendPeriod  contendPeriod;

    private volatile ScheduledFuture<?>       contendFuture;
    private          ScheduledExecutorService contendScheduler;
    private          RScript                  luaScript;
    private          RTopic                   mutexTopic;
    private          RTopic                   contenderTopic;
    private          MutexEventLister         lister;

    public RedisContendController(MutexContender contender,
                                  Executor executor,
                                  Duration ttl,
                                  Duration transition,
                                  RedissonClient redissonClient) {
        super(contender, executor);
        this.redissonClient   = redissonClient;
        this.ttl              = ttl;
        this.transition       = transition;
        this.contendPeriod    = new ContendPeriod(contender.getMutex());
        this.keys             = new String[]{"{" + contender.getMutex() + "}"};
        this.mutexChannel     = Strings.lenientFormat("%s:{%s}", KEY_PREFIX, contender.getMutex());
        this.contenderChannel = Strings.lenientFormat("%s:%s", this.mutexChannel, contender.getContenderId());
    }

    private void mutexSubscribe() {
        this.luaScript      = redissonClient.getScript(new StringCodec());
        this.lister         = new MutexEventLister();
        this.mutexTopic     = redissonClient.getTopic(mutexChannel, new StringCodec());
        this.contenderTopic = redissonClient.getTopic(contenderChannel, new StringCodec());
        mutexTopic.addListener(String.class, lister);
        contenderTopic.addListener(String.class, lister);
    }

    private void mutexUnsubscribe() {
        this.mutexTopic.removeListener(lister);
        this.contenderTopic.removeListener(lister);
    }

    @Override
    protected void startContend() {
        mutexSubscribe();
        //竞争资源调度器
        contendScheduler = new ScheduledThreadPoolExecutor(1,
                                                           Threads.threadFactory(Strings.lenientFormat(
                                                                   "redis_contender_%s_%s",
                                                                   getContender().getMutex(),
                                                                   getContender().getContenderId())),
                                                           new ThreadPoolExecutor.DiscardPolicy());
        nextSchedule(0L);
    }

    @Override
    protected void stopContend() {
        if (this.contendFuture != null) {
            this.contendFuture.cancel(true);
        }
        if (contendScheduler != null) {
            this.contendScheduler.shutdown();
        }
        this.mutexUnsubscribe();
        this.mutexRelease();
    }

    private void nextSchedule(long nextDelay) {
        this.contendFuture = contendScheduler.schedule(() -> {
            try {
                if (isOwner()) {
                    this.mutexGuard();
                    return;
                }
                this.mutexAcquire();
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        }, nextDelay, TimeUnit.MILLISECONDS);
    }

    private void notifyAndNextSchedule(AcquiredResult result) {
        try {
            final MutexOwner mutexOwner = newMutexOwner(result);
            notifyOwner(mutexOwner);
            final long nextDelay = contendPeriod.ensureNextDelay(mutexOwner);
            nextSchedule(nextDelay);
        } catch (Exception error) {
            log.error(error.getMessage(), error);
            nextSchedule(ttl.toMillis());
        }
    }

    private void mutexGuard() {
        Object[] values = {this.contender.getContenderId(), String.valueOf(ttl.toMillis())};
        final String result = this.luaScript.eval(RScript.Mode.READ_WRITE,
                                                  GUARD_SCRIPT,
                                                  RScript.ReturnType.STATUS,
                                                  Arrays.asList(keys),
                                                  values);
        if (log.isDebugEnabled()) {
            log.debug("mutex guard result:{}", result);
        }
        this.notifyAndNextSchedule(JSON.parseObject(result, AcquiredResult.class));
    }

    private void mutexAcquire() {
        Object[] values = {this.contender.getContenderId(), String.valueOf(ttl.toMillis() + transition.toMillis())};
        final String result = this.luaScript.eval(RScript.Mode.READ_WRITE,
                                                  ACQUIRE_SCRIPT,
                                                  RScript.ReturnType.STATUS,
                                                  Arrays.asList(keys),
                                                  values);
        if (log.isDebugEnabled()) {
            log.debug("mutex acquired result:{}", result);
        }
        this.notifyAndNextSchedule(JSON.parseObject(result, AcquiredResult.class));
    }

    private MutexOwner newMutexOwner(AcquiredResult result) {
        return newMutexOwner(result.getOwnerId(), result.getTransitionAt());
    }

    private MutexOwner newMutexOwner(String ownerId, long transitionAt) {
        final long ttlAt      = transitionAt - transition.toMillis();
        final long acquiredAt = ttlAt - ttl.toMillis();
        return new MutexOwner(ownerId, acquiredAt, ttlAt, transitionAt);
    }

    private void mutexRelease() {
        final Boolean result = this.luaScript.eval(RScript.Mode.READ_WRITE,
                                                   RELEASE_SCRIPT,
                                                   RScript.ReturnType.BOOLEAN,
                                                   Arrays.asList(keys),
                                                   this.contender.getContenderId());
        if (result) {
            notifyOwner(MutexOwner.NONE);
        }
    }

    private static String loadLua(String luaName) {
        try {
            final URL resource = Resources.getResource(luaName);
            return Resources.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new RuntimeException(error.getMessage(), error);
        }
    }


    public class MutexEventLister implements MessageListener<String> {

        private long getTransitionAt(MutexEvent event) {
            return event.getEventAt() + ttl.toMillis() + transition.toMillis();
        }

        @Override
        public void onMessage(CharSequence channel, String event) {
            if (!channel.toString().startsWith(mutexChannel)) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("redis channel [{}] mutex event:{}", channel, event);
            }
            final MutexEvent mutexEvent = JSON.parseObject(event, MutexEvent.class);
            switch (mutexEvent.getEvent()) {
                case MutexEvent.ACQUIRED:
                    notifyOwner(newMutexOwner(mutexEvent.getOwnerId(), getTransitionAt(mutexEvent)));
                    break;
                case MutexEvent.RELEASED:
                    notifyOwner(MutexOwner.NONE);
                    mutexAcquire();
                    break;
                default:
                    throw new IllegalStateException("unknown event:" + mutexEvent.getEvent());
            }
        }
    }

}
