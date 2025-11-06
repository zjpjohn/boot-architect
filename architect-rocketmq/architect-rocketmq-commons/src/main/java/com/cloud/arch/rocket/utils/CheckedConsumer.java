package com.cloud.arch.rocket.utils;

import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T> {

    void accept(T t) throws Throwable;

    static <T> Consumer<T> apply(CheckedConsumer<T> consumer) {
        Objects.requireNonNull(consumer);
        return t -> {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

}
