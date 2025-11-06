package com.cloud.arch.mutex;


import com.cloud.arch.mutex.core.ContendController;
import com.cloud.arch.mutex.core.ContendControllerFactory;
import com.cloud.arch.mutex.core.ContendMutexProps;
import com.cloud.arch.mutex.core.MutexContender;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class JdbcContendControllerFactory implements ContendControllerFactory {

    private final IMutexOwnerRepository mutexOwnerRepository;
    private final Executor              executor;

    public JdbcContendControllerFactory(IMutexOwnerRepository mutexOwnerRepository) {
        this(mutexOwnerRepository, ForkJoinPool.commonPool());
    }

    public JdbcContendControllerFactory(IMutexOwnerRepository mutexOwnerRepository, Executor executor) {
        this.mutexOwnerRepository = mutexOwnerRepository;
        this.executor             = executor;
    }

    @Override
    public ContendController createContendController(MutexContender contender, ContendMutexProps props) {
        return new JdbcContendController(contender, executor, mutexOwnerRepository, props.getInitialDelay(), props.getTtl(), props.getTransition());
    }

}
