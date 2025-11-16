package com.boot.architect.infrast.facade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
public class PrepareEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String name = event.getSpringApplication().getMainApplicationClass().getPackage().getName();
        log.info("web-example current application package:{}", name);
    }
}
