package com.cloud.arch.cache.support;

import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 缓存淘汰事件发布器，提供 {@link #publish} 实例方法供业务系统手动淘汰缓存，
 * 通过 {@link ApplicationContext} 发布 {@link CacheEvictEvent} 触发延迟双删流程
 */
public class CacheEvictPublisher implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * 发布缓存淘汰事件，提供给业务系统手动淘汰缓存时调用
     */
    public void publish(CacheEvictEvent... events) {
        if (events == null) {
            return;
        }
        for (CacheEvictEvent evictEvent : events) {
            context.publishEvent(evictEvent);
        }
    }

}
