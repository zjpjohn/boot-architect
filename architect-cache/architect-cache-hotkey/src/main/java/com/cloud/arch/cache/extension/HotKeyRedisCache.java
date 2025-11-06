package com.cloud.arch.cache.extension;

import com.cloud.arch.cache.core.*;
import com.cloud.arch.cache.metrics.StatsCounter;
import com.cloud.arch.hotkey.core.key.DetectedValue;
import com.cloud.arch.hotkey.core.key.HotKeyCache;
import com.google.common.collect.MapMaker;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.EntryExpiredListener;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings("unchecked")
public class HotKeyRedisCache extends AbstractValueAdaptCache implements CacheTtlRefreshTask {

    // 缓存加载锁前缀
    private static final String CACHE_LOAD_PATTERN            = "hk:load:%s:%s";
    // 缓存刷新锁前缀
    private static final String REFRESH_LOCK_PATTERN          = "hk:refresh:%s:%s";
    // 加载缓存锁时间
    private static final long   CACHE_LOAD_LOCK_TIME          = 350L;
    // 刷新缓存锁时间
    private static final long   CACHE_REFRESH_LOCK_TIME       = 20L;
    // 缓存竞争锁失败加载间隔时间
    private static final long   CACHE_LOCK_FAIL_LOAD_INTERVAL = 50L;
    // 缓存竞争锁失败重试获取次数
    private static final long   CACHE_LOCK_FAIL_LOAD_RETRY    = 5;

    // 本地缓存加载锁
    private final ConcurrentMap<Object, Object> keyLocks = new MapMaker().weakValues().makeMap();
    // 缓存配置信息
    private final CacheSettings                 settings;
    // 随机时间生成器
    private final Random                        random;
    // redis缓存
    private final RMapCache<Object, Object>     mapCache;
    // 缓存刷新
    private final RefreshPolicy                 refreshPolicy;
    // redisson客户端
    private final RedissonClient                redissonClient;
    // 热点数据缓存
    private final HotKeyCache                   hotKeyCache;
    // 二级缓存ttl刷新器
    private final RemoteCacheTtlRefresher       ttlRefresher;
    // 缓存统计时间ticker
    private       Ticker                        statsTicker;
    // 缓存统计计数器
    private       StatsCounter                  statsCounter;

    public HotKeyRedisCache(String name,
                            CacheSettings settings,
                            RefreshPolicy refreshPolicy,
                            RedissonClient redissonClient,
                            RemoteCacheTtlRefresher ttlRefresher,
                            HotKeyCache hotKeyCache) {
        super(name, settings.isAllowNullValue());
        this.settings       = settings;
        this.random         = new Random();
        this.refreshPolicy  = refreshPolicy;
        this.redissonClient = redissonClient;
        this.mapCache       = redissonClient.getMapCache(this.getName());
        this.hotKeyCache    = hotKeyCache;
        this.ttlRefresher   = ttlRefresher;
        this.cacheExpireListener();
    }

    @Override
    public void statsTicker(Ticker ticker) {
        this.statsTicker = ticker;
    }

    @Override
    public Ticker statsTicker() {
        if (this.statsTicker != null) {
            return this.statsTicker;
        }
        return Ticker.disableTicker();
    }

    @Override
    public void statsCounter(StatsCounter statsCounter) {
        this.statsCounter = statsCounter;
    }

    @Override
    public StatsCounter statsCounter() {
        if (this.statsCounter != null) {
            return this.statsCounter;
        }
        return StatsCounter.disabledStatsCounter();
    }

    /**
     * redis缓存数据过期监听，删除本地热点数据
     */
    private void cacheExpireListener() {
        this.mapCache.addListener((EntryExpiredListener<Object, Object>) event -> {
            // redis缓存失效->本地热点缓存
            if (log.isInfoEnabled()) {
                log.info("redis second cache[{}]-->key[{}] value expired.", this.getName(), event.getKey());
            }
            statsCounter().recordEvict(1);
            this.hotKeyCache.remove(this.getName(), event.getKey().toString());
        });
    }

    @Override
    public long cacheSize() {
        return this.mapCache.size();
    }

    @Override
    public <T> T get(Object key) {
        // 热点数据探测
        DetectedValue detected = hotKeyCache.get(this.getName(), key.toString());
        // 热点探测失败或非热点key直接从redis中获取缓存数据
        if (!detected.isSuccess() || !detected.isHot()) {
            return (T) getAndRefresh(key);
        }
        if (!detected.isCached()) {
            // 首次探测到热key本地没有缓存数据，从redis缓存中加载本地缓存
            // 注意存在并发问题，所以加锁缓存到本地
            return this.lockAndLoadHotValue(key);
        }
        // 命中本地热点缓存
        statsCounter().recordHits(1, true);
        return detected.toValue();
    }

    /**
     * 获取缓存数据并刷新redis缓存数据时间
     *
     * @param key 缓存key
     */
    private Object getAndRefresh(Object key) {
        // 获取缓存数据
        Object value = this.mapCache.get(key);
        if (value == null) {
            statsCounter().recordMisses(1);
            return null;
        }
        statsCounter().recordHits(1, false);
        if (this.settings.isEnableRefresh() && !(value instanceof NullValue)) {
            this.ttlRefresher.refreshTtl(this.getName(), key, value, this);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        // 先探测热key
        DetectedValue detected = hotKeyCache.get(this.getName(), key.toString());
        // 热key探测失败或非热点数据
        if (!detected.isSuccess() || !detected.isHot()) {
            Object value = this.mapCache.get(key);
            // 没有缓存数据
            if (value == null) {
                return this.loadAndPut(key, valueLoader);
            }
            statsCounter().recordHits(1, false);
            // 开启刷新缓存数据且缓存数据非空
            if (this.settings.isEnableRefresh() && !(value instanceof NullValue)) {
                this.ttlRefresher.refreshTtl(this.getName(), key, value, this);
            }
            return (T) toValue(value);
        }
        if (!detected.isCached()) {
            // 首次探测到热点数据没有加载数据，进行热点数据加载到本地
            // 注意存在并发问题，所以加锁缓存到本地
            return this.lockAndLoadHotValue(key);
        }
        // 命中本地热点缓存数据
        statsCounter().recordHits(1, true);
        return detected.toValue();
    }

    /**
     * 加锁并加载热点数据到本地缓存
     *
     * @param key 缓存key
     */
    private <T> T lockAndLoadHotValue(Object key) {
        synchronized (keyLocks.computeIfAbsent(key, k -> new Object())) {
            DetectedValue detected = hotKeyCache.get(this.getName(), key.toString());
            if (detected.isCached()) {
                statsCounter().recordHits(1, true);
                return detected.toValue();
            }
            Object value = this.getAndRefresh(key);
            if (value != null) {
                this.hotKeyCache.put(this.getName(), key.toString(), value);
            }
            return (T) toValue(value);
        }
    }

    /**
     * 加载并更新指定key的redis缓存数据
     *
     * @param key         缓存key
     * @param valueLoader 缓存数据加载器
     */
    private <T> T loadAndPut(Object key, Callable<T> valueLoader) {
        String lockKey = String.format(CACHE_LOAD_PATTERN, this.getName(), key);
        RLock  lock    = redissonClient.getLock(lockKey);
        try {
            boolean lockedSuccess = lock.tryLock(CACHE_LOAD_LOCK_TIME, TimeUnit.MILLISECONDS);
            if (!lockedSuccess) {
                Object value = this.lockFailRetryLoad(key);
                return (T) toValue(value);
            }
            try {
                return doLoadAndPut(key, valueLoader);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    private <T> T doLoadAndPut(Object key, Callable<T> valueLoader) {
        try {
            Object value = this.mapCache.get(key);
            // 两种情况:数据本身不为空；数据为空值NullValue
            if (value != null) {
                statsCounter().recordHits(1, false);
                return (T) toValue(value);
            }
            T loadValue = statsWrappedLoad(valueLoader);
            this.put(key, loadValue);
            return loadValue;
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("lock load and put data key [{}] error:", key, e);
            }
        }
        return null;
    }

    /**
     * 加锁失败重试获取缓存数据
     *
     * @param key 缓存key
     */
    private Object lockFailRetryLoad(Object key) throws InterruptedException {
        Object value     = null;
        int    retryTime = 0;
        do {
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

    @Override
    public void put(Object key, Object value) {
        Object result = toStoreValue(value);
        if (result != null) {
            // 过期时间=固定时间+随机时间
            long expireTime = settings.getExpire() + this.random.nextInt(settings.getRandomBound());
            /* 缩短空值缓存时间 */
            if (isAllowNullValue() && result instanceof NullValue) {
                expireTime = expireTime / settings.getMagnification();
            }
            try {
                this.mapCache.put(key, result, expireTime, TimeUnit.SECONDS);
            } catch (Exception error) {
                log.warn("put key[{}] cached value to redis cache[{}] error:", key, this.getName(), error);
            }
        }
        // 清除本地热点缓存数据
        this.hotKeyCache.remove(this.getName(), key.toString());
        // 发送清除本地热点数据事件
        this.refreshPolicy.sendEvict(this.getName(), key);
    }

    @Override
    public void evict(Object key) {
        try {
            statsCounter().recordEvict(1);
            this.mapCache.remove(key);
        } catch (Exception error) {
            log.warn("clear cache[{}] key[{}] cached value error:", this.getName(), key, error);
        }
        // 清除本地热点缓存数据
        this.hotKeyCache.remove(this.getName(), key.toString());
        // 发送清除热点数据事件
        this.refreshPolicy.sendEvict(this.getName(), key);
    }

    @Override
    public void clear() {
        try {
            statsCounter().recordEvict(this::cacheSize);
            this.mapCache.clear();
        } catch (Exception error) {
            log.warn("clear cache[{}] all value error:", this.getName(), error);
        }
        // 清除本地缓存所有热点数据
        this.hotKeyCache.removeAll(this.getName());
        // 发送清除所有热点数据事件
        this.refreshPolicy.sendClear(this.getName());
    }

    /**
     * 刷新延长redis缓存过期时间
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    @Override
    public void refreshTtl(Object key, Object value) {
        long ttl = this.mapCache.remainTimeToLive(key);
        if (ttl > 0 && ttl <= this.settings.getPreloadTime()) {
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
     * 刷新缓存过期时间
     */
    private void refreshCacheTtl(Object key, Object value) {
        try {
            // 重新计算缓存过期时间
            int  randomBound = this.settings.getRandomBound();
            long expire      = this.settings.getExpire() + this.random.nextInt(randomBound);
            this.mapCache.put(key, value, expire, TimeUnit.MILLISECONDS);
        } catch (Exception error) {
            if (log.isInfoEnabled()) {
                log.info(error.getMessage(), error);
            }
        }
    }
}
