package com.cloud.arch.rocket.commons;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.ClassUtils;

@Slf4j
public class RocketPrepareEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public static String DEFAULT_BASE_PACKAGE = "";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        if (StringUtils.isBlank(DEFAULT_BASE_PACKAGE)) {
            DEFAULT_BASE_PACKAGE = ClassUtils.getPackageName(event.getSpringApplication().getMainApplicationClass());
            log.info("rocketmq queue current application package:{}", DEFAULT_BASE_PACKAGE);
        }
    }
}
