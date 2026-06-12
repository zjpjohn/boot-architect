package com.cloud.arch.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.api.atomic.LongIncrementArgs;

import java.time.Duration;
import java.util.Collections;

@Slf4j
public class AtomicCounter {

    private static final String INCR_AND_EXPIRE = "local v = redis.call('INCRBY', KEYS[1], ARGV[1]); if tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; return v";

    private static final String DECR_AND_EXPIRE = "local v = redis.call('DECRBY', KEYS[1], ARGV[1]); if tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; return v";

    private static final String INCR_FIXED_WINDOW = "local exists = redis.call('EXISTS', KEYS[1]); local v = redis.call('INCRBY', KEYS[1], ARGV[1]); if exists == 0 and tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; return v";

    private static final String DECR_FIXED_WINDOW = "local exists = redis.call('EXISTS', KEYS[1]); local v = redis.call('DECRBY', KEYS[1], ARGV[1]); if exists == 0 and tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; return v";

    private static final String GET_AND_DEL = "local v = redis.call('GET', KEYS[1]); if v then redis.call('DEL', KEYS[1]) end; return v";

    private final RedissonClient redissonClient;
    private final String         keyPrefix;

    public AtomicCounter(RedissonClient redissonClient) {
        this(redissonClient, null);
    }

    public AtomicCounter(RedissonClient redissonClient, String keyPrefix) {
        this.redissonClient = redissonClient;
        if (keyPrefix != null && !keyPrefix.isEmpty()) {
            this.keyPrefix = keyPrefix.endsWith(":") ? keyPrefix : keyPrefix + ":";
        } else {
            this.keyPrefix = "";
        }
    }

    /**
     * 原子递增指定值，永不过期
     */
    public long incr(String key, long delta) {
        return redissonClient.getAtomicLong(resolveKey(key)).incrementAndGet(LongIncrementArgs.by(delta));
    }

    /**
     * 原子递增 1，永不过期
     */
    public long incr(String key) {
        return redissonClient.getAtomicLong(resolveKey(key)).incrementAndGet();
    }

    /**
     * 原子递减 1，永不过期
     */
    public long decr(String key) {
        return redissonClient.getAtomicLong(resolveKey(key)).decrementAndGet();
    }

    /**
     * 原子递增指定值并刷新 TTL（滑动窗口计数），每次调用都重置过期时间
     */
    public long incr(String key, long delta, Duration ttl) {
        return eval(INCR_AND_EXPIRE, RScript.ReturnType.LONG, resolveKey(key), delta, ttl.toMillis());
    }

    /**
     * 原子递增 1 并刷新 TTL（滑动窗口计数），每次调用都重置过期时间
     */
    public long incr(String key, Duration ttl) {
        return eval(INCR_AND_EXPIRE, RScript.ReturnType.LONG, resolveKey(key), 1, ttl.toMillis());
    }

    /**
     * 原子递减指定值并刷新 TTL（滑动窗口计数），每次调用都重置过期时间
     */
    public long decr(String key, long delta, Duration ttl) {
        return eval(DECR_AND_EXPIRE, RScript.ReturnType.LONG, resolveKey(key), delta, ttl.toMillis());
    }

    /**
     * 原子递减 1 并刷新 TTL（滑动窗口计数），每次调用都重置过期时间
     */
    public long decr(String key, Duration ttl) {
        return eval(DECR_AND_EXPIRE, RScript.ReturnType.LONG, resolveKey(key), 1, ttl.toMillis());
    }

    /**
     * 原子递增指定值，仅在 key 不存在时设置 TTL（固定窗口计数），到期自动清零
     */
    public long incrIfAbsent(String key, long delta, Duration ttl) {
        return eval(INCR_FIXED_WINDOW, RScript.ReturnType.LONG, resolveKey(key), delta, ttl.toMillis());
    }

    /**
     * 原子递增 1，仅在 key 不存在时设置 TTL（固定窗口计数），到期自动清零
     */
    public long incrIfAbsent(String key, Duration ttl) {
        return eval(INCR_FIXED_WINDOW, RScript.ReturnType.LONG, resolveKey(key), 1, ttl.toMillis());
    }

    /**
     * 原子递减指定值，仅在 key 不存在时设置 TTL（固定窗口计数），到期自动清零
     */
    public long decrIfAbsent(String key, long delta, Duration ttl) {
        return eval(DECR_FIXED_WINDOW, RScript.ReturnType.LONG, resolveKey(key), delta, ttl.toMillis());
    }

    /**
     * 原子递减 1，仅在 key 不存在时设置 TTL（固定窗口计数），到期自动清零
     */
    public long decrIfAbsent(String key, Duration ttl) {
        return eval(DECR_FIXED_WINDOW, RScript.ReturnType.LONG, resolveKey(key), 1, ttl.toMillis());
    }

    /**
     * 获取当前计数值，key 不存在返回 null，不修改 TTL
     */
    public Long get(String key) {
        return redissonClient.<Long>getBucket(resolveKey(key)).get();
    }

    /**
     * 设置计数值并指定 TTL
     */
    public void set(String key, long value, Duration ttl) {
        redissonClient.getBucket(resolveKey(key)).set(value, ttl);
    }

    /**
     * 原子获取当前值并重置为零（删除 key），key 不存在返回 null
     */
    public Long getAndReset(String key) {
        return eval(GET_AND_DEL, RScript.ReturnType.VALUE, resolveKey(key));
    }

    /**
     * 删除计数器，返回 true 表示 key 存在并被删除
     */
    public boolean delete(String key) {
        return redissonClient.getBucket(resolveKey(key)).delete();
    }

    private String resolveKey(String key) {
        return keyPrefix + key;
    }

    private Long eval(String script, RScript.ReturnType returnType, String key, Object... args) {
        return redissonClient.getScript()
                             .eval(RScript.Mode.READ_WRITE, script, returnType, Collections.singletonList(key), args);
    }

}
