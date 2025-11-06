package com.cloud.arch.transaction.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;


@Slf4j
public class ApplicationContextHolder {

    private static ApplicationContext context;

    /**
     * 获取指定类型的Bean
     */
    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }
}
