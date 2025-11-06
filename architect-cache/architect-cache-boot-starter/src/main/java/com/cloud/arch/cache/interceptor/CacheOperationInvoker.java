package com.cloud.arch.cache.interceptor;

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
