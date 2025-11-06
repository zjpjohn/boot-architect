package com.cloud.arch.cache.expression;

import java.lang.reflect.Method;

public record CacheExpressionRootObject(Method method, Object[] args, Object target, Class<?> targetClass) {

}
