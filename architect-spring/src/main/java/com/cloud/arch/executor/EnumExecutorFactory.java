package com.cloud.arch.executor;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
public class EnumExecutorFactory<K extends Enum<K>, E extends Executor<K>>
        implements SmartInitializingSingleton, ApplicationContextAware {


    private final EnumMap<K, E>      executors;
    private final Class<E>           executorType;
    private       ApplicationContext applicationContext;

    public EnumExecutorFactory(@NonNull Class<K> keyType, @NonNull Class<E> executorType) {
        Preconditions.checkState(executorType.isInterface(), "executor type must be interface.");
        this.executorType = executorType;
        this.executors    = new EnumMap<>(keyType);
    }

    /**
     * 获取指定业务标识的执行器
     * 执行器不允许为空
     *
     * @param bizIndex 业务标识
     */
    public E of(K bizIndex) {
        E executor = this.executors.get(bizIndex);
        Assert.notNull(executor, String.format("%s executor not exist", bizIndex.toString()));
        return executor;
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

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, E> beans = this.applicationContext.getBeansOfType(this.executorType);
        if (CollectionUtils.isEmpty(beans)) {
            log.warn("no implemented beans of executor {} exist.", this.executorType.getName());
            return;
        }
        beans.forEach((key, value) -> executors.put(value.bizIndex(), value));
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
