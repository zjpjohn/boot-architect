package com.cloud.arch.transaction.core;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class AsyncTxInvokers {

    private static final Map<String, AsyncTxInvoker> invokers = new ConcurrentHashMap<>();

    public static void add(AsyncTxInvoker invoker) {
        String asyncKey = invoker.getKey();
        if (invokers.get(asyncKey) != null) {
            String message = String.format("Has exist same key[%s] async task , please confirm correct config.",
                                           asyncKey);
            throw new IllegalArgumentException(message);
        }
        invokers.put(asyncKey, invoker);
    }

    public static AsyncTxInvoker get(String key) {
        return invokers.get(key);
    }

}
