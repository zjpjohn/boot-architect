package com.cloud.arch.cache.interceptor;

/**
 * 缓存操作回调接口，封装目标方法的实际调用，将受检异常包装为 {@link ThrowableWrapper} 抛出
 */
@FunctionalInterface
public interface CacheOperationInvoker {

    Object invoke() throws ThrowableWrapper;

    class ThrowableWrapper extends RuntimeException {

        private static final long serialVersionUID = -4941293743723659161L;

        private final Throwable original;

        public ThrowableWrapper(Throwable original) {
            super(original.getMessage(), original);
            this.original = original;
        }

        public Throwable getOriginal() {
            return this.original;
        }
    }

}
