package com.cloud.arch.cache.support;

import java.lang.reflect.Method;

@FunctionalInterface
public interface KeyGenerator {

    Object generate(Object target, Method method, Object... params);
}
