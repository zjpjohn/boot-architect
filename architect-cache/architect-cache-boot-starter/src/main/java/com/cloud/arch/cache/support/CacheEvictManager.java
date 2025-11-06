package com.cloud.arch.cache.support;

import com.cloud.arch.cache.config.CloudCacheProperties;
import com.cloud.arch.cache.core.Cache;
import com.cloud.arch.cache.core.CacheManager;
import com.cloud.arch.cache.utils.CacheThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CacheEvictManager implements DisposableBean, InitializingBean {

    private static final long DEFAULT_UNTIL_TIME = TimeUnit.HOURS.toMillis(1);

    private final AtomicBoolean              startState = new AtomicBoolean(false);
    private final CacheManager               cacheManager;
    private final DelayQueue<CacheEvictTask> delayQueue;
    private final Thread                     triggerWorker;
    private final CloudCacheProperties       properties;

    public CacheEvictManager(CacheManager cacheManager, CloudCacheProperties properties) {
        this.cacheManager  = cacheManager;
        this.properties    = properties;
        this.delayQueue    = new DelayQueue<>();
        this.triggerWorker = new Thread(this::triggerDelayEvict, "cache-evict-trigger-thread");
    }

    /**
     * 触发延迟删除缓存
     */
    private void triggerDelayEvict() {
        do {
            try {
                CacheEvictTask request = this.delayQueue.poll(DEFAULT_UNTIL_TIME, TimeUnit.MILLISECONDS);
                while (request != null) {
                    CacheEvictTask command = request;
                    this.doCacheEvict(command.getEvent());
                    request = this.delayQueue.take();
                }
            } catch (InterruptedException error) {
                log.warn("trigger delay cache evict task interrupted exception:", error);
                Thread.currentThread().interrupt();
            }
        }
        while (this.startState.get());
    }

    /**
     * 删除淘淘缓存
     *
     * @param event 删除缓存事件
     */
    private void doCacheEvict(CacheEvictEvent event) {
        CacheThreadPoolExecutor.run(() -> {
            Cache cache = cacheManager.getCache(event.getName());
            if (cache != null) {
                try {
                    Object key = event.isEvictAll() ? null : event.getKey();
                    cache.evictOrClear(key);
                } catch (Exception error) {
                    log.error("cache evict error:", error);
                }
            }
        });
    }

    /**
     * 普通方法缓存淘汰监听
     */
    @TransactionalEventListener(fallbackExecution = true, classes = CacheEvictEvent.class)
    public void cacheEvict(CacheEvictEvent event) {
        // 立即删除缓存
        this.doCacheEvict(event);
        // 延迟再次删除缓存,解决小概率缓存不一致问题
        if (this.properties.isEnableDelayEvict() && event.isDelayEvict()) {
            this.publishDelayEvict(event);
        }
    }

    /**
     * 发布延迟删除缓存事件
     */
    private void publishDelayEvict(CacheEvictEvent event) {
        long           evictAt        = System.currentTimeMillis() + this.properties.getDelayEvictInterval();
        CacheEvictTask cacheEvictTask = new CacheEvictTask(event, evictAt);
        this.delayQueue.add(cacheEvictTask);
    }

    @Override
    public void destroy() throws Exception {
        this.startState.set(false);
        if (!this.triggerWorker.isInterrupted()) {
            this.triggerWorker.interrupt();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.startState.compareAndSet(false, true)) {
            this.triggerWorker.start();
        }
    }

}
