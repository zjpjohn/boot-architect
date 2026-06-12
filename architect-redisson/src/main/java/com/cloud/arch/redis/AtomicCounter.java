package com.cloud.arch.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collections;

@Slf4j
public class AtomicCounter {

    private static final String INCR_AND_EXPIRE = "local v = redis.call('INCRBY', KEYS[1], ARGV[1]); if tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; return v";

    private static final String DECR_AND_EXPIRE = "local v = redis.call('DECRBY', KEYS[1], ARGV[1]); if tonumber(ARGV[2]) > 0 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end; return v";

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

    public long incrAndGet(String key, long delta, long ttlMillis) {
        return eval(INCR_AND_EXPIRE, RScript.ReturnType.LONG, resolveKey(key), delta, ttlMillis);
    }

    public long decrAndGet(String key, long delta, long ttlMillis) {
        return eval(DECR_AND_EXPIRE, RScript.ReturnType.LONG, resolveKey(key), delta, ttlMillis);
    }

    public Long get(String key) {
        return redissonClient.<Long>getBucket(resolveKey(key)).get();
    }

    public void set(String key, long value, long ttlMillis) {
        redissonClient.getBucket(resolveKey(key)).set(value, Duration.ofMillis(ttlMillis));
    }

    public Long getAndReset(String key) {
        return eval(GET_AND_DEL, RScript.ReturnType.VALUE, resolveKey(key));
    }

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
