package com.cloud.token.plugin;

import com.cloud.token.muted.IMutedRepository;
import com.cloud.token.utils.TokenConstants;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

public record RedisMutedRepository(RedissonClient redissonClient) implements IMutedRepository {

    @Override
    public void save(String key, int level, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        RBucket<Integer> bucket = redissonClient.getBucket(key);
        if (timeout == TokenConstants.NEVER_EXPIRE) {
            bucket.set(level);
            return;
        }
        bucket.set(level, Duration.ofHours(timeout));
    }

    @Override
    public boolean isMuted(String key, int level) {
        RBucket<Integer> bucket = redissonClient.getBucket(key);
        Integer          value  = bucket.get();
        if (value == TokenConstants.NO_MUTED_LEVEL) {
            return false;
        }
        return value >= level;
    }

    @Override
    public boolean cancel(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public int getMuted(String key) {
        RBucket<Integer> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

}
