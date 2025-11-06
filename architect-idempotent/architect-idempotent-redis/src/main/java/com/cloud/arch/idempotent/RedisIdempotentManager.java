package com.cloud.arch.idempotent;

import com.cloud.arch.idempotent.support.IdempotentInfo;
import com.cloud.arch.idempotent.support.IdempotentManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.lang.management.ManagementFactory;

@Slf4j
public record RedisIdempotentManager(RedissonClient redissonClient) implements IdempotentManager {

    @Override
    public boolean tryAcquire(IdempotentInfo idempotent) {
        return redissonClient.getBucket(idempotent.key()).setIfAbsent(generateValue(), idempotent.duration());
    }

    @Override
    public void completed(IdempotentInfo idempotent, Throwable throwable) {
        if (idempotent.removeNow()) {
            redissonClient.getBucket(idempotent.key()).delete();
        }
    }

    private String generateValue() {
        long pid = ManagementFactory.getRuntimeMXBean().getPid();
        return pid + "_" + Thread.currentThread().threadId();
    }

}
