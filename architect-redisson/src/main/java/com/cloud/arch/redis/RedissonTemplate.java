package com.cloud.arch.redis;

import org.redisson.api.*;
import org.redisson.client.codec.Codec;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record RedissonTemplate(RedissonClient redissonClient) {

    public <T> RBucket<T> getBuket(String key) {
        return redissonClient.getBucket(key);
    }

    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public <T> void set(String key, T obj) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(obj);
    }

    public <T> void set(String key, T obj, Long expire) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(obj, Duration.ofSeconds(expire));
    }

    public <T> T getAndDel(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.getAndDelete();
    }

    public boolean del(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public <T> void set(String key, T obj, Long expire, TimeUnit timeUnit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(obj, Duration.ofMillis(timeUnit.toMillis(expire)));
    }

    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    public <K, V> V put(String key, K mKey, V value) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.put(mKey, value);
    }

    public <V> V remove(String key, String mKey) {
        RMap<String, V> map = redissonClient.getMap(key);
        return map.remove(mKey);
    }

    public <K, V> RMapCache<K, V> getMapCache(String key) {
        return redissonClient.getMapCache(key);
    }

    public <K, V> V put(String key, K mKey, V value, long expire, TimeUnit unit) {
        RMapCache<K, V> mapCache = getMapCache(key);
        return mapCache.put(mKey, value, expire, unit);
    }

    public <K, V> V get(String key, K mKey) {
        RMapCache<K, V> cache = getMapCache(key);
        return cache.get(mKey);
    }

    public <V> V removeCache(String key, String mKey) {
        RMapCache<String, V> cache = redissonClient.getMapCache(key);
        return cache.remove(mKey);
    }

    public <V> RList<V> getList(String key) {
        return redissonClient.getList(key);
    }

    public <V> RSet<V> getSet(String key) {
        return redissonClient.getSet(key);
    }

    public <V> RSortedSet<V> getSortedSet(String key) {
        return redissonClient.getSortedSet(key);
    }

    public <V> RScoredSortedSet<V> getScoredSortedSet(String key) {
        return redissonClient.getScoredSortedSet(key);
    }

    public <K, V> RListMultimap<K, V> getMultiMap(String key) {
        return redissonClient.getListMultimap(key);
    }

    public <K, V> boolean putMulti(String key, K mKey, V value) {
        RListMultimap<K, V> multimap = redissonClient.getListMultimap(key);
        return multimap.put(mKey, value);
    }

    public <K, V> boolean putAll(String key, K mKey, List<V> values) {
        RListMultimap<K, V> multimap = redissonClient.getListMultimap(key);
        return multimap.putAll(mKey, values);
    }

    public RLock getLock(String lockName) {
        return redissonClient.getLock(lockName);
    }

    public RReadWriteLock getRWLock(String lockName) {
        return redissonClient.getReadWriteLock(lockName);
    }

    public <V> RPriorityQueue<V> priorityQueue(String name) {
        return redissonClient.getPriorityQueue(name);
    }

    public RLongAdder longAdder(String key) {
        return redissonClient.getLongAdder(key);
    }

    public RAtomicLong atomicLong(String key) {
        return redissonClient.getAtomicLong(key);
    }

    public Long incrAndGet(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }

    public Long decrAndGet(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.decrementAndGet();
    }

    public RTopic topic(String name) {
        return redissonClient.getTopic(name);
    }

    public RTopic topic(String name, Codec codec) {
        return redissonClient.getTopic(name, codec);
    }

    public RBatch batch(BatchOptions options) {
        return redissonClient.createBatch(options);
    }

    public RBatch batch() {
        return redissonClient.createBatch();
    }

    public RScript luaScript() {
        return redissonClient.getScript();
    }

    public RScript luaScript(Codec codec) {
        return redissonClient.getScript(codec);
    }

}
