package com.cloud.arch.rocket.commons;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

public abstract class BaseOnsConfiguration {

    @Bean(name = {"org.springframework.context.annotation.internalScheduledAnnotationProcessor"})
    @ConditionalOnMissingBean(ScheduledAnnotationBeanPostProcessor.class)
    public ScheduledAnnotationBeanPostProcessor scheduledAnnotationProcessor() {
        return new ScheduledAnnotationBeanPostProcessor();
    }

}
