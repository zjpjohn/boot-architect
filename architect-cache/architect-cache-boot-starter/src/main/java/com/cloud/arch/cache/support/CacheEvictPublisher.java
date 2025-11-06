package com.cloud.arch.cache.support;

import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CacheEvictPublisher implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static void publish(CacheEvictEvent... events) {
        if (events == null) {
            return;
        }
        for (CacheEvictEvent evictEvent : events) {
            context.publishEvent(evictEvent);
        }
    }

}
