package com.cloud.arch.event.commons;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nonnull;

/**
 * Spring 容器持有者，为静态工具方法提供 ApplicationContext 引用，用于获取 Bean 和发布本地领域事件。
 */
@Slf4j
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
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
            // Bean 不存在返回 null，无需抛出异常
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
