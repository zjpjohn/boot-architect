package com.cloud.arch.mutex.core;

public interface ContendControllerFactory {

    ContendController createContendController(MutexContender contender,ContendMutexProps props);

}
