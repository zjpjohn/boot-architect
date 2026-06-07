package com.cloud.arch.cache.warmup.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 缓存预热分布式协调器，基于 Redisson 分布式锁按缓存名粒度加锁
 */
@Slf4j
public class WarmUpCoordinator {

    private static final String LOCK_PREFIX = "cache:warmup:lock:";

    private final RedissonClient redissonClient;

    public WarmUpCoordinator(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryAcquireWarmUpLock(String cacheName, long waitSeconds) {
        if (redissonClient == null) {
            return true;
        }
        RLock lock = redissonClient.getLock(LOCK_PREFIX + cacheName);
        try {
            boolean acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
            if (acquired) {
                if (log.isInfoEnabled()) {
                    log.info("[WarmUp] acquired distributed lock for cache={}", cacheName);
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("[WarmUp] skipped, another node is handling cache={}", cacheName);
                }
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[WarmUp] lock acquisition interrupted for cache={}", cacheName);
            return false;
        }
    }

    public void releaseWarmUpLock(String cacheName) {
        if (redissonClient == null) {
            return;
        }
        RLock lock = redissonClient.getLock(LOCK_PREFIX + cacheName);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
