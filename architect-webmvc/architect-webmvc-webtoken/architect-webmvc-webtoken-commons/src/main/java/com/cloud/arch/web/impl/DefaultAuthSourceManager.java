package com.cloud.arch.web.impl;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.IHttpAuthSource;
import com.cloud.arch.web.IHttpAuthSourceManager;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class DefaultAuthSourceManager
        implements IHttpAuthSourceManager, ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext context;

    private final Map<String, IHttpAuthSource> sourceCache = Maps.newHashMap();

    @Override
    public void addSource(IHttpAuthSource source) {
        this.sourceCache.put(source.value(), source);
    }

    @Override
    public Map<String, IHttpAuthSource> ofList() {
        return Collections.unmodifiableMap(sourceCache);
    }

    @Override
    public IHttpAuthSource ofKey(String domain) {
        return sourceCache.get(domain);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            Collection<IHttpAuthSource> sources = this.context.getBeansOfType(IHttpAuthSource.class).values();
            if (CollectionUtils.isNotEmpty(sources)) {
                sources.forEach(this::addSource);
            }
        } catch (BeansException error) {
            log.warn("no {} beans founded.", IHttpAuthSource.class.getName());
        }
    }

}
