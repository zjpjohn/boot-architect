package com.cloud.token.plugin;

import com.cloud.token.dual.IDualSafeRepository;
import com.cloud.token.utils.TokenConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;

@Slf4j
@AllArgsConstructor
public class RedisSafeRepository implements IDualSafeRepository {

    private final RedissonClient redissonClient;

    @Override
    public void save(String key, long timeout) {
        if (timeout == 0 || timeout <= TokenConstants.NO_EXPIRE_VALUE) {
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(TokenConstants.AUTH_SAFE_VALUE, Duration.ofSeconds(timeout));
    }

    @Override
    public boolean isSafe(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return StringUtils.isNotBlank(bucket.get());
    }

}
