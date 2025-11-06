package com.cloud.arch.utils;

import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * 获取指定类型的Bean
     */
    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    /**
     * 获取指定类型的bean，不存在返回null
     */
    public static <T> T getNullableBean(Class<T> type) {
        try {
            return context.getBean(type);
        } catch (BeansException ignored) {
        }
        return null;
    }

    /**
     * 发布领域事件
     */
    public static void publishEvent(Object event) {
        context.publishEvent(event);
    }

}
