package com.cloud.arch.mutex.utils;

import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;

@UtilityClass
public final class Systems {

    public static String currentProcessName() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }

    public static long currentProcessId() {
        String processName  = currentProcessName();
        String processIdStr = processName.split("@")[0];
        return Long.parseLong(processIdStr);
    }

}
