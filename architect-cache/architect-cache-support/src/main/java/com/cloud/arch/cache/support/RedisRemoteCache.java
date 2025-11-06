package com.cloud.arch.cache.support;

import com.cloud.arch.cache.core.*;
import com.cloud.arch.cache.metrics.StatsCounter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.EntryExpiredListener;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisRemoteCache extends AbstractRemoteCache implements CacheTtlRefreshTask {

    // 缓存加载锁前缀
    private static final String                    CACHE_LOAD_PATTERN            = "t:cache:%s:%s";
    // 缓存刷新锁前缀
    private static final String                    REFRESH_LOCK_PATTERN          = "t:refresh:%s:%s";
    // 加载缓存锁时间
    private static final long                      CACHE_LOAD_LOCK_TIME          = 200L;
    // 刷新缓存锁时间
    private static final long                      CACHE_REFRESH_LOCK_TIME       = 20L;
    // 缓存竞争锁失败加载间隔时间
    private static final long                      CACHE_LOCK_FAIL_LOAD_INTERVAL = 50L;
    // 缓存竞争锁失败重试获取次数
    private static final long                      CACHE_LOCK_FAIL_LOAD_RETRY    = 5;
    // 随机时间生成器
    private final        Random                    random;
    // redis缓存
    private final        RMapCache<Object, Object> mapCache;
    // redis操作模板
    private final        RedissonClient            redissonClient;
    // 缓存刷新器
    private final        RemoteCacheTtlRefresher   ttlRefresher;
    // 缓存统计时间ticker
    private              Ticker                    statsTicker;
    // 缓存统计计数器
    private              StatsCounter              statsCounter;

    public RedisRemoteCache(String name,
                            CacheSettings settings,
                            RedissonClient redissonClient,
                            RemoteCacheTtlRefresher ttlRefresher) {
        super(name, settings);
        this.random         = new Random();
        this.redissonClient = redissonClient;
        this.mapCache       = redissonClient.getMapCache(this.getName());
        this.ttlRefresher   = ttlRefresher;
        this.remoteCacheExpireListener();
    }

    /**
     * 注册redis缓存过期监听器
     */
    private void remoteCacheExpireListener() {
        // 监听redis二级缓存数据过期，删除本地缓存数据
        this.mapCache.addListener((EntryExpiredListener<Object, Object>) event -> {
            if (log.isInfoEnabled()) {
                log.info("Redis second cache[{}]-->key[{}] value expired.", this.getName(), event.getKey());
            }
            statsCounter().recordEvict(1);
            // 存在本地缓存，本地缓存淘汰
            AbstractLocalCache localCache = this.getLocalCache();
            if (localCache != null) {
                localCache.doEvict(event.getKey());
            }
        });
    }

    @Override
    public void statsTicker(Ticker ticker) {
        this.statsTicker = ticker;
    }

    @Override
    public void statsCounter(StatsCounter statsCounter) {
        this.statsCounter = statsCounter;
    }

    /**
     * time ticker used by stats counter
     */
    @Override
    public Ticker statsTicker() {
        if (this.statsTicker != null) {
            return this.statsTicker;
        }
        return Ticker.disableTicker();
    }

    /**
     * stats counter
     */
    @Override
    public StatsCounter statsCounter() {
        if (this.statsCounter != null) {
            return this.statsCounter;
        }
        return StatsCounter.disabledStatsCounter();
    }

    /**
     * 缓存容量
     */
    @Override
    public long cacheSize() {
        return this.mapCache.size();
    }

    /**
     * 查询指定key的redis缓存
     *
     * @param key 缓存key
     */
    @Override
    public Object doGet(Object key) {
        Object value = this.mapCache.get(key);
        if (value == null) {
            statsCounter().recordMisses(1);
            return null;
        }
        statsCounter().recordHits(1, false);
        if (this.getSettings().isEnableRefresh() && !(value instanceof NullValue)) {
            // 异步刷新非空缓存
            this.ttlRefresher.refreshTtl(this.getName(), key, value, this);
        }
        return value;
    }

    /**
     * 查询指定缓存key的redis缓存数据
     *
     * @param key         缓存key
     * @param valueLoader 缓存数据加载器
     */
    @Override
    public Object doGet(Object key, Callable<?> valueLoader) {
        Object value = this.mapCache.get(key);
        if (value == null) {
            return loadAndPut(key, valueLoader);
        }
        statsCounter().recordHits(1, false);
        if (this.getSettings().isEnableRefresh() && !(value instanceof NullValue)) {
            this.ttlRefresher.refreshTtl(this.getName(), key, value, this);
        }
        return value;
    }

    /**
     * 加载并更新指定key的redis缓存数据
     *
     * @param key         缓存key
     * @param valueLoader 缓存数据加载器
     */
    private Object loadAndPut(Object key, Callable<?> valueLoader) {
        String lockKey = String.format(CACHE_LOAD_PATTERN, this.getName(), key);
        RLock  lock    = redissonClient.getLock(lockKey);
        try {
            boolean lockedSuccess = lock.tryLock(CACHE_LOAD_LOCK_TIME, TimeUnit.MILLISECONDS);
            if (!lockedSuccess) {
                return this.retryLoadValue(key);
            }
            try {
                return doLoadAndPut(key, valueLoader);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private Object doLoadAndPut(Object key, Callable<?> valueLoader) {
        try {
            Object value = this.mapCache.get(key);
            if (value != null) {
                statsCounter().recordHits(1, false);
                return value;
            }
            // 两种情况:数据本身不为空；数据为空值NullValue
            Object userValue = statsWrappedLoad(valueLoader);
            value = toStoreValue(userValue);
            this.doPut(key, value);
            return value;
        } catch (Exception error) {
            if (log.isWarnEnabled()) {
                log.warn(error.getMessage(), error);
            }
        }
        return null;
    }

    /**
     * 当竞争获取缓存加载锁失败时，重试获取已缓存的数据 重试5次获取缓存值，每次间隔50毫秒
     *
     * @param key 缓存key
     */
    private Object retryLoadValue(Object key) throws InterruptedException {
        Object value     = null;
        int    retryTime = 0;
        do {
            // 先等待指定加载时间，在重试加载数据
            TimeUnit.MILLISECONDS.sleep(CACHE_LOCK_FAIL_LOAD_INTERVAL);
            value     = this.mapCache.get(key);
            retryTime = retryTime + 1;
            if (value != null) {
                statsCounter().recordHits(1, false);
                break;
            }
        }
        while (retryTime < CACHE_LOCK_FAIL_LOAD_RETRY);
        return value;
    }

    /**
     * 更新缓存key的缓存值
     *
     * @param key   缓存key
     * @param value 缓存数据
     */
    @Override
    public void doPut(Object key, Object value) {
        if (value != null) {
            CacheSettings settings = this.getSettings();
            // 过期时间=固定时间+随机时间
            long expireTime = settings.getExpire() + this.random.nextInt(settings.getRandomBound());
            /* 缩短空值缓存时间 */
            if (isAllowNullValue() && value instanceof NullValue) {
                expireTime = expireTime / settings.getMagnification();
            }
            this.mapCache.put(key, value, expireTime, TimeUnit.SECONDS);
        }
    }

    /**
     * 删除指定缓存key的redis缓存
     *
     * @param key 缓存key
     */
    @Override
    public void doEvict(Object key) {
        if (log.isInfoEnabled()) {
            log.info("evict redis cache[{}] key[{}]", this.getName(), key);
        }
        statsCounter().recordEvict(1);
        this.mapCache.fastRemove(key);
    }

    /**
     * 清空redis缓存
     */
    @Override
    public void doClear() {
        if (log.isInfoEnabled()) {
            log.info("clear redis cache[{}] all values", this.getName());
        }
        statsCounter().recordEvict(this::cacheSize);
        this.mapCache.clear();
    }

    /**
     * 延长redis缓存过期时间
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    @Override
    public void refreshTtl(Object key, Object value) {
        // 异步刷新缓存过期时间
        if (this.canRefreshRemainTime(key)) {
            String lockKey = String.format(REFRESH_LOCK_PATTERN, this.getName(), key);
            RLock  lock    = redissonClient.getLock(lockKey);
            try {
                if (!lock.tryLock(CACHE_REFRESH_LOCK_TIME, TimeUnit.MILLISECONDS)) {
                    return;
                }
                try {
                    this.refreshCacheTtl(key, value);
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 刷新缓存ttl
     */
    private void refreshCacheTtl(Object key, Object value) {
        try {
            int  randomBound = this.getSettings().getRandomBound();
            long expire      = this.getSettings().getExpire() + this.random.nextInt(randomBound);
            this.mapCache.put(key, value, expire, TimeUnit.SECONDS);
        } catch (Exception error) {
            if (log.isWarnEnabled()) {
                log.warn("cache[{}] refresh key [{}] error:", this.getName(), key, error);
            }
        }
    }

    /**
     * 判断是否为可刷新缓存剩余时间
     *
     * @param key 缓存key
     */
    private boolean canRefreshRemainTime(Object key) {
        long ttl = this.mapCache.remainTimeToLive(key);
        return ttl > 0 && ttl < this.getSettings().getPreloadTime();
    }

}
