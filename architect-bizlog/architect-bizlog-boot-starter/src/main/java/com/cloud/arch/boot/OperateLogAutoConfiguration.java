package com.cloud.arch.boot;


import com.cloud.arch.annotations.OperateLog;
import com.cloud.arch.core.IFunctionFactory;
import com.cloud.arch.core.INamedFunction;
import com.cloud.arch.core.IOperatorFunction;
import com.cloud.arch.core.LogFunctionContainer;
import com.cloud.arch.core.impl.DefaultFunctionFactory;
import com.cloud.arch.core.impl.DefaultOperatorFunction;
import com.cloud.arch.props.OperateLoggerProperties;
import com.cloud.arch.repository.DefaultLogRepository;
import com.cloud.arch.repository.ILogRepository;
import com.cloud.arch.support.core.AsyncLogDispatcher;
import com.cloud.arch.support.core.LogOperateContextFactory;
import com.cloud.arch.support.core.LogOperateInterceptor;
import com.cloud.arch.support.core.ProxyLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

import java.util.List;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties(OperateLoggerProperties.class)
public class OperateLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IOperatorFunction.class)
    public IOperatorFunction operatorFunction() {
        return new DefaultOperatorFunction();
    }

    @Bean
    @ConditionalOnMissingBean(ILogRepository.class)
    public ILogRepository logRepository() {
        return new DefaultLogRepository();
    }

    @Bean
    public LogFunctionContainer logFunctionContainer(
            @Autowired List<INamedFunction> namedFunctions) {
        return new LogFunctionContainer(namedFunctions);
    }

    @Bean
    public IFunctionFactory functionFactory(LogFunctionContainer logFunctionContainer) {
        return new DefaultFunctionFactory(logFunctionContainer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.logger", name = "async", havingValue = "true")
    public AsyncLogDispatcher asyncLogDispatcher(ILogRepository logRepository, OperateLoggerProperties properties) {
        return new AsyncLogDispatcher(logRepository, properties);
    }

    @Bean
    public ProxyLogRepository proxyLogRepository(ILogRepository logRepository,
                                                 ObjectProvider<AsyncLogDispatcher> asyncLogDispatcher) {
        AsyncLogDispatcher dispatcher = asyncLogDispatcher.stream().findFirst().orElse(null);
        return new ProxyLogRepository(logRepository, dispatcher);
    }

    @Bean
    public LogOperateContextFactory operateContextFactory(IOperatorFunction operatorFunction,
                                                          IFunctionFactory functionFactory) {
        return new LogOperateContextFactory(operatorFunction, functionFactory);
    }

    @Bean
    public LogOperateInterceptor logOperateInterceptor(LogOperateContextFactory operateContextFactory,
                                                       ProxyLogRepository proxyLogRepository) {
        return new LogOperateInterceptor(operateContextFactory, proxyLogRepository);
    }

    @Bean
    public DefaultPointcutAdvisor operateLogAdvisor(LogOperateInterceptor logOperateInterceptor) {
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
        advisor.setAdvice(logOperateInterceptor);
        advisor.setPointcut(AnnotationMatchingPointcut.forMethodAnnotation(OperateLog.class));
        advisor.setOrder(Ordered.LOWEST_PRECEDENCE);
        return advisor;
    }

}
