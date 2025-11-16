package com.cloud.arch.rocket.producer.spring;

import com.cloud.arch.rocket.commons.RocketPrepareEventListener;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

public class RocketProducerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private static final String            MULTI_PACKAGE_DELIMITER = ",";
    private static final String            BASE_PACKAGES_KEY       = "com.cloud.rocketmq.producer.base-packages";
    private static final String            BASE_PACKAGES_KEY_CAMEL = "com.cloud.rocketmq.producer.basePackages";
    private final        BeanNameGenerator beanNameGenerator       = new AnnotationBeanNameGenerator();

    private Environment    environment;
    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathProducerScanner scanner  = new ClassPathProducerScanner(registry, environment, resourceLoader);
        Set<String>              packages = getPackages();
        packages.stream().map(scanner::findCandidateComponents).filter(e -> e instanceof AnnotatedBeanDefinition)
                .forEach(e -> registerProxyProducer(registry, (AnnotatedBeanDefinition) e));
    }

    public Set<String> getPackages() {
        String packages = environment.getProperty(BASE_PACKAGES_KEY);
        if (StringUtils.isBlank(packages)) {
            packages = environment.getProperty(BASE_PACKAGES_KEY_CAMEL);
        }
        if (StringUtils.isBlank(packages)) {
            packages = RocketPrepareEventListener.DEFAULT_BASE_PACKAGE;
        }
        return Sets.newHashSet(packages.split(MULTI_PACKAGE_DELIMITER));
    }

    public void registerProxyProducer(BeanDefinitionRegistry registry, AnnotatedBeanDefinition beanDefinition) {
        String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
        AbstractBeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(RocketProducerFactoryBean.class)
                                                                 .addPropertyValue("name", beanName).addPropertyValue(
                        "type",
                        beanDefinition.getBeanClassName()).setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                                                                 .getBeanDefinition();
        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, beanName);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
