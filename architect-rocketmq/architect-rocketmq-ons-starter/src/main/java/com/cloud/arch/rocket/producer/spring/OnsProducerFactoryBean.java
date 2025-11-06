package com.cloud.arch.rocket.producer.spring;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Proxy;

@Slf4j
public class OnsProducerFactoryBean implements FactoryBean<Object>, ApplicationContextAware {

    private ApplicationContext context;
    private Class<?>           type;
    private String             name;

    @Override
    public Object getObject() throws Exception {
        ProxyProducerProvider bean = this.context.getBean(ProxyProducerProvider.class);
        return Proxy.newProxyInstance(type.getClassLoader(),
                new Class[]{type},
                bean.newInstance(type));
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @SneakyThrows
    public void setType(String type) {
        this.type = Class.forName(type);
    }

    public void setName(String name) {
        this.name = name;
    }

}
