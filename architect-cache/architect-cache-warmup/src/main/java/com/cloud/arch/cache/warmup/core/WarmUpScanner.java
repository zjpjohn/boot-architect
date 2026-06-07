package com.cloud.arch.cache.warmup.core;

import com.cloud.arch.cache.annotations.CacheResult;
import com.cloud.arch.cache.warmup.config.WarmUpProperties;
import com.cloud.arch.cache.warmup.support.WarmUpMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预热任务扫描器，在容器启动完成后扫描所有 @CacheResult(warmup=true) 的方法并注册到 WarmUpRegistry
 */
@Slf4j
public class WarmUpScanner implements SmartInitializingSingleton, ApplicationContextAware {

    private       ConfigurableApplicationContext applicationContext;
    private final WarmUpRegistry                 registry;
    private final WarmUpExecutor                 executor;
    private final WarmUpArgsProvider             argsProvider;
    private final WarmUpProperties               properties;
    private final WarmUpMetrics                  metrics;

    public WarmUpScanner(WarmUpRegistry registry,
                         WarmUpExecutor executor,
                         WarmUpArgsProvider argsProvider,
                         WarmUpProperties properties,
                         WarmUpMetrics metrics) {
        this.registry = registry;
        this.executor = executor;
        this.argsProvider = argsProvider;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] auto warm-up is disabled");
            }
            return;
        }
        scan();
        if (registry.cacheNameCount() == 0) {
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] no @CacheResult(warmup=true) methods found");
            }
            return;
        }

        Set<String> targetCaches = filterCaches(registry.getCacheNames());
        if (targetCaches.isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info("[WarmUp] no caches to warm up (all filtered out)");
            }
            return;
        }

        if (properties.isAsync()) {
            com.cloud.arch.cache.utils.CacheThreadPoolExecutor.run(() -> doExecute(targetCaches));
        } else {
            doExecute(targetCaches);
        }
    }

    private void scan() {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        String[]                        beanNames   = beanFactory.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            if (beanName.startsWith("scopedTarget.")) {
                continue;
            }
            Class<?> targetType = beanFactory.getType(beanName);
            if (targetType == null || isSpringContainerClass(targetType)) {
                continue;
            }
            processBean(beanName, targetType);
        }
        if (log.isInfoEnabled()) {
            log.info("[WarmUp] scanned {} beans, registered {} cache names",
                     beanNames.length,
                     registry.cacheNameCount());
        }
    }

    private void processBean(String beanName, Class<?> targetType) {
        Class<?> userClass = ClassUtils.getUserClass(targetType);
        Map<Method, Set<CacheResult>> annotatedMethods = MethodIntrospector.selectMethods(userClass,
                                                                                          (MethodIntrospector.MetadataLookup<Set<CacheResult>>) method -> {
                                                                                              Set<CacheResult> annotations = AnnotatedElementUtils.findAllMergedAnnotations(
                                                                                                      method,
                                                                                                      CacheResult.class);
                                                                                              if (CollectionUtils.isEmpty(
                                                                                                      annotations)) {
                                                                                                  return null;
                                                                                              }
                                                                                              // 过滤掉 warmup=false 的
                                                                                              annotations.removeIf(cr -> !cr.warmup());
                                                                                              return CollectionUtils.isEmpty(
                                                                                                      annotations) ? null : annotations;
                                                                                          });

        if (annotatedMethods.isEmpty()) {
            return;
        }

        Object targetBean        = applicationContext.getBean(beanName);
        String effectiveBeanName = !targetBean.getClass().equals(userClass) ? userClass.getSimpleName() : beanName;

        annotatedMethods.forEach((method, cacheResults) -> {
            for (CacheResult cr : cacheResults) {
                WarmUpTask task = new WarmUpTask();
                task.setCacheNames(cr.names());
                task.setBeanName(effectiveBeanName);
                task.setTargetBean(targetBean);
                task.setMethod(method);
                for (String name : cr.names()) {
                    task.setCacheName(name);
                    registry.register(name, task);
                }
            }
        });
    }

    private Set<String> filterCaches(Set<String> allCacheNames) {
        Set<String> filter = properties.getCaches();
        if (CollectionUtils.isEmpty(filter)) {
            return allCacheNames;
        }
        Set<String> result = new LinkedHashSet<>();
        for (String name : allCacheNames) {
            if (filter.contains(name)) {
                result.add(name);
            }
        }
        return result;
    }

    private void doExecute(Set<String> cacheNames) {
        if (log.isInfoEnabled()) {
            log.info("[WarmUp] starting auto warm-up, {} caches", cacheNames.size());
        }
        long               start   = System.currentTimeMillis();
        List<WarmUpResult> results = new ArrayList<>();
        for (String cacheName : cacheNames) {
            List<Object[]>     args  = argsProvider.provide(cacheName);
            List<WarmUpTask>   tasks = registry.getTasksByCache(cacheName);
            List<WarmUpResult> r     = executor.executeAll(cacheName, args, tasks);
            if (r != null) {
                results.addAll(r);
            }
        }
        long duration = System.currentTimeMillis() - start;
        if (log.isInfoEnabled()) {
            log.info("[WarmUp] auto warm-up completed in {}ms", duration);
        }
        metrics.report(results);
    }

    private boolean isSpringContainerClass(Class<?> clazz) {
        return clazz.getName().startsWith("org.springframework.");
    }
}
