
package com.cloud.arch.mutex.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;

public class Threads {

    public static ThreadFactory threadFactory(String name) {
        return new ThreadFactoryBuilder()
                .setDaemon(false)
                .setNameFormat(name + "-%d")
                .build();
    }
}
