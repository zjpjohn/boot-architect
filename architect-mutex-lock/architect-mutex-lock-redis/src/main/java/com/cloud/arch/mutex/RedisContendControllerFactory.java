package com.cloud.arch.mutex;


import com.cloud.arch.mutex.core.ContendController;
import com.cloud.arch.mutex.core.ContendControllerFactory;
import com.cloud.arch.mutex.core.ContendMutexProps;
import com.cloud.arch.mutex.core.MutexContender;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class RedisContendControllerFactory implements ContendControllerFactory {

    private final MutexRedisSupplier redissonLoader;
    private final Executor           executor;

    public RedisContendControllerFactory(MutexRedisSupplier redissonLoader) {
        this(redissonLoader, ForkJoinPool.commonPool());
    }

    public RedisContendControllerFactory(MutexRedisSupplier redissonLoader, Executor executor) {
        this.redissonLoader = redissonLoader;
        this.executor       = executor;
    }

    @Override
    public ContendController createContendController(MutexContender contender, ContendMutexProps props) {
        return new RedisContendController(contender, executor, props.getTtl(), props.getTransition(), redissonLoader.get());
    }

}
