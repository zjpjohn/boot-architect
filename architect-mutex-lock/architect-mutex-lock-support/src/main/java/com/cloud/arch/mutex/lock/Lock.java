package com.cloud.arch.mutex.lock;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public interface Lock extends AutoCloseable{

    void acquire();

    void acquire(Duration timeout) throws TimeoutException;

}
