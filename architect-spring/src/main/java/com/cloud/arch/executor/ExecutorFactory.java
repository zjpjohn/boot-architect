package com.cloud.arch.executor;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import lombok.NonNull;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ExecutorFactory implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext context;

    private final Map<Class<? extends Comparable>, ExecutorContainer> containers = Maps.newHashMap();

    /**
     * 获取指定业务标识的执行器
     */
    public <K extends Comparable<K>, E extends Executor<K>> E of(K key) {
        Class<K>                typeClass = (Class<K>) key.getClass();
        ExecutorContainer<K, E> container = containers.get(typeClass);
        if (container == null) {
            throw new IllegalArgumentException(String.format("%s executor not configured.", typeClass.getSimpleName()));
        }
        return Preconditions.checkNotNull(container.of(key), String.format("%s executor not exist", key));
    }

    /**
     * 获取指定业务标识的执行器
     */
    public <K extends Comparable<K>, E extends Executor<K>> Optional<E> ofNullable(K key) {
        Class<K> typeClass = (Class<K>) key.getClass();
        return Optional.ofNullable(containers.get(typeClass)).flatMap(e -> e.ofNullable(key));
    }

    /**
     * 获取指定类型的全部执行器
     */
    public <K extends Comparable<K>, E extends Executor<K>> Collection<E> executors(Class<K> type) {
        return Optional.ofNullable(containers.get(type))
                       .map(ExecutorContainer::executors)
                       .orElseGet(Collections::emptyList);
    }

    /**
     * 获取指定类型已配置执行的key集合
     */
    public <K extends Comparable<K>> Set<K> keys(Class<K> type) {
        return Optional.ofNullable(containers.get(type)).map(ExecutorContainer::keys).orElseGet(Collections::emptySet);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        ClassIndex.getAnnotated(ExecutorPoint.class).forEach(e -> parseAndRegistry((Class<Executor>) e));
    }

    private <K extends Comparable<K>, E extends Executor<K>> void parseAndRegistry(Class<E> executorType) {
        Type                    type      = executorType.getGenericInterfaces()[0];
        Class<K>                argument  = (Class<K>) ((ParameterizedType) type).getActualTypeArguments()[0];
        Map<String, E>          beans     = context.getBeansOfType(executorType);
        ExecutorContainer<K, E> container = new ExecutorContainer<>(beans.values().stream().toList());
        containers.put(argument, container);
    }

    public static class ExecutorContainer<K extends Comparable<K>, E extends Executor<K>> {

        private final Map<K, E> executors = Maps.newHashMap();

        public ExecutorContainer(List<E> beans) {
            beans.forEach(e -> executors.put(e.bizIndex(), e));
        }

        /**
         * 获取指定业务标识的执行器
         * 执行器不允许为空
         *
         * @param bizIndex 业务标识
         */
        public E of(K bizIndex) {
            E executor = this.executors.get(bizIndex);
            return Preconditions.checkNotNull(executor, String.format("%s executor not exist", bizIndex));
        }

        /**
         * 获取指定业务标识的执行器
         * 允许执行器为空
         *
         * @param bizIndex 业务标识
         */
        public Optional<E> ofNullable(K bizIndex) {
            return Optional.ofNullable(this.executors.get(bizIndex));
        }

        /**
         * 执行器集合
         */
        public Collection<E> executors() {
            return this.executors.values();
        }

        /**
         * 业务标识集合
         */
        public Set<K> keys() {
            return this.executors.keySet();
        }

    }

}
