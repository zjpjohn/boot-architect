package com.cloud.arch.aggregate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {

    private volatile Supplier<T> supplier;
    private volatile T           result;

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    private Lazy(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Lazy function is <null>.");
    }

    private void forceEagerEvaluation() {
        if (supplier != null) {
            synchronized (this) {
                if (supplier != null) {
                    result   = supplier.get();
                    supplier = null;
                }
            }
        }
    }

    @Override
    public T get() {
        forceEagerEvaluation();
        return result;
    }

    public boolean isAvailable() {
        return supplier == null;
    }

    public Optional<T> getNullable() {
        return isAvailable() ? Optional.ofNullable(get()) : Optional.empty();
    }

    public void ifPresent(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "Consumer of lazy value is <null>.");
        if (isAvailable()) {
            consumer.accept(get());
        }
    }

    public <U> Lazy<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function is <null>.");
        return of(() -> mapper.apply(get()));
    }

    public <U> Lazy<U> flatMap(Function<? super T, ? extends Supplier<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "Mapper function is <null>.");
        return of(() -> Objects.requireNonNull(mapper.apply(get()), "Lazy supplier is <null>.").get());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (isAvailable() ? "[" + get() + ']' : "[not yet resolved]");
    }

}
