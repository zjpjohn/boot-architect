package com.cloud.arch.aggregate;

import java.io.Serializable;

public class AggregateFactory {

    private AggregateFactory() {
        throw new UnsupportedOperationException("this is a factory class, please use static method.");
    }

    /**
     * 聚合构建工厂方法
     *
     * @param root 聚合根
     * @param <R>  聚合根类型
     */
    public static <I extends Serializable, R extends AggregateRoot<I>> Aggregate<I, R> create(R root) {
        return new Aggregate<>(root, ForyDeepCopier.instance());
    }

    /**
     * 聚合构建工厂
     *
     * @param root       聚合根对象
     * @param repository 聚合根仓储
     */
    public static <I extends Serializable, R extends AggregateRoot<I>> Aggregate<I, R> create(R root,
                                                                                              Repository<I, R> repository) {
        return new Aggregate<>(root, ForyDeepCopier.instance(), repository);
    }

    /**
     * 聚合构建工厂
     *
     * @param root   聚合根对象
     * @param copier 对象深度拷贝
     */
    public static <I extends Serializable, R extends AggregateRoot<I>> Aggregate<I, R> create(R root,
                                                                                              DeepCopier copier) {
        return new Aggregate<>(root, copier);
    }

    /**
     * @param root       聚合根对象
     * @param copier     聚合根对象拷贝器
     * @param repository 聚合根仓储
     */
    public static <I extends Serializable, R extends AggregateRoot<I>> Aggregate<I, R> create(R root,
                                                                                              DeepCopier copier,
                                                                                              Repository<I, R> repository) {
        return new Aggregate<>(root, copier, repository);
    }

}
