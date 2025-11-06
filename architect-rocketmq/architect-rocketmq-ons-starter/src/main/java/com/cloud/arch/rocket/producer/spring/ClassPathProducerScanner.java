package com.cloud.arch.rocket.producer.spring;

import com.cloud.arch.rocket.annotations.Producer;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;


public class ClassPathProducerScanner extends ClassPathBeanDefinitionScanner {

    public ClassPathProducerScanner(BeanDefinitionRegistry registry,
                                    Environment environment,
                                    ResourceLoader resourceLoader) {
        super(registry, false, environment, resourceLoader);
        this.addIncludeFilter(new AnnotationTypeFilter(Producer.class));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        return metadata.isIndependent() && metadata.isInterface();
    }
}
