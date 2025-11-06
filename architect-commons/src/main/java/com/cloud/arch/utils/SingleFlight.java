package com.cloud.arch.utils;

import com.google.common.collect.Maps;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

public class SingleFlight<K, R> {

    private final ConcurrentMap<K, Caller<R>> callers = Maps.newConcurrentMap();

    public R execute(K key, Callable<R> callable) throws Exception {
        boolean[] result = {false};
        Caller<R> caller = this.callers.computeIfAbsent(key, k -> {
            result[0] = true;
            return new Caller<>();
        });
        if (!result[0]) {
            return caller.await();
        }
        try {
            return caller.execute(callable);
        } finally {
            callers.remove(key);
        }
    }

    private static class Caller<V> {

        private final Object    lock     = new Object();
        private       boolean   finished = false;
        private       V         result   = null;
        private       Exception error    = null;

        V await() throws Exception {
            synchronized (lock) {
                while (!finished) {
                    lock.wait();
                }
                if (error != null) {
                    throw error;
                }
                return result;
            }
        }

        V execute(Callable<V> callable) throws Exception {
            V         result = null;
            Exception error  = null;
            try {
                result = callable.call();
                return result;
            } catch (Exception exception) {
                error = exception;
                throw exception;
            } finally {
                finish(result, error);
            }
        }

        void finish(V result, Exception error) {
            synchronized (lock) {
                this.error    = error;
                this.result   = result;
                this.finished = true;
                lock.notifyAll();
            }
        }
    }

}
