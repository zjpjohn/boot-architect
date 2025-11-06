package com.cloud.arch.cache.interceptor.operation;

import org.springframework.context.expression.AnnotatedElementKey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class CacheOperationKey implements Comparable<CacheOperationKey> {

    private final AbsCacheOperation<? extends Annotation> operation;
    private final AnnotatedElementKey                     methodKey;

    public CacheOperationKey(AbsCacheOperation<? extends Annotation> operation, Method method, Class<?> targetClass) {
        this.operation = operation;
        this.methodKey = new AnnotatedElementKey(method, targetClass);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CacheOperationKey)) {
            return false;
        }
        CacheOperationKey otherKey = (CacheOperationKey) other;
        return (this.operation.equals(otherKey.operation) &&
                this.methodKey.equals(otherKey.methodKey));
    }

    @Override
    public int hashCode() {
        return (this.operation.hashCode() * 31 + this.methodKey.hashCode());
    }

    @Override
    public String toString() {
        return this.operation + " on " + this.methodKey;
    }

    @Override
    public int compareTo(CacheOperationKey other) {
        int result = this.operation.getName().compareTo(other.operation.getName());
        if (result == 0) {
            result = this.methodKey.compareTo(other.methodKey);
        }
        return result;
    }

}
