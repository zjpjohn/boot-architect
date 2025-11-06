package com.cloud.arch.event.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;

public class Threads {

    public static ThreadFactory threadFactory(String domain) {
        String nameFormat = domain + "-%d";
        return new ThreadFactoryBuilder().setDaemon(false).setNameFormat(nameFormat).build();
    }
}
