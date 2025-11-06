package com.cloud.arch.cache.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.cloud.arch.cache.support.CacheAnnotationParser.CACHE_OPERATION_ANNOTATIONS;


@Slf4j
public class CacheOperationMethodProcessor
        implements SmartInitializingSingleton, ApplicationContextAware, BeanFactoryPostProcessor {

    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

    private ConfigurableListableBeanFactory beanFactory;
    private ConfigurableApplicationContext  applicationContext;
    private CacheOperationSource            cacheOperationSource;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext, "ApplicationContext does not implement ConfigurableApplicationContext");
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Assert.state(this.beanFactory != null, "No ConfigurableListableBeanFactory set");
        Assert.state(this.applicationContext != null, "No ApplicationContext set");
        this.cacheOperationSource = this.getBean(CacheOperationCachedSource.class);
        Assert.state(this.cacheOperationSource != null, "No CacheOperationSource bean founded.");
        //处理所有bean缓存操作
        this.processAllBeanCacheOperations();
        //构建缓存操作构建缓存
        this.cacheOperationSource.cacheBuild();
    }

    protected <T> T getBean(Class<T> expectedType) {
        try {
            return this.applicationContext.getBean(expectedType);
        } catch (BeansException error) {
            log.error(error.getMessage(), error);
        }
        return null;
    }


    private static boolean isSpringContainerClass(Class<?> clazz) {
        if (clazz.getName().startsWith("org.springframework.")) {
            return true;
        }
        Class<?> userClass = ClassUtils.getUserClass(clazz);
        return !AnnotatedElementUtils.isAnnotated(userClass, Component.class)
               || AnnotatedElementUtils.isAnnotated(userClass, Configuration.class);
    }

    private void processAllBeanCacheOperations() {
        String[] beanNames = beanFactory.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            if (ScopedProxyUtils.isScopedTarget(beanName)) {
                continue;
            }
            Class<?> targetType = AutoProxyUtils.determineTargetClass(beanFactory, beanName);
            if (targetType == null) {
                continue;
            }
            if (ScopedObject.class.isAssignableFrom(targetType)) {
                String   targetBeanName = ScopedProxyUtils.getTargetBeanName(beanName);
                Class<?> targetClass    = AutoProxyUtils.determineTargetClass(beanFactory, targetBeanName);
                if (targetClass != null) {
                    targetType = targetClass;
                }
            }
            processCacheOperations(beanName, targetType);
        }
    }

    private void processCacheOperations(final String beanName, final Class<?> targetType) {
        Class<?> userClass = ClassUtils.getUserClass(targetType);
        if (!this.nonAnnotatedClasses.contains(userClass)
            && !isSpringContainerClass(userClass)
            && AnnotationUtils.isCandidateClass(userClass, CACHE_OPERATION_ANNOTATIONS)) {

            Map<Method, Set<? extends Annotation>> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(userClass, metadataLookup());
            } catch (Throwable ex) {
                log.warn("Could not resolve methods for bean with name '" + beanName + "'", ex);
            }
            if (CollectionUtils.isEmpty(annotatedMethods)) {
                this.nonAnnotatedClasses.add(targetType);
                return;
            }
            annotatedMethods.forEach((method, annotations) -> {
                cacheOperationSource.cacheOperations(userClass, method, annotations);
            });
        }
    }


    private MethodIntrospector.MetadataLookup<Set<? extends Annotation>> metadataLookup() {
        return method -> {
            Set<Annotation> annotations
                    = AnnotatedElementUtils.findAllMergedAnnotations(method, CACHE_OPERATION_ANNOTATIONS);
            if (CollectionUtils.isEmpty(annotations)) {
                annotations = null;
            }
            return annotations;
        };
    }

}
