package com.cloud.arch.support.core;

import com.cloud.arch.core.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;


@Slf4j
public class LogOperateInterceptor implements MethodInterceptor, EnvironmentAware, InitializingBean {

    private final LogOperateContextFactory operateContextFactory;
    private final ProxyLogRepository       proxyLogRepository;
    private       Environment              environment;
    private       String                   application;

    public LogOperateInterceptor(LogOperateContextFactory operateContextFactory,
                                 ProxyLogRepository proxyLogRepository) {
        this.operateContextFactory = operateContextFactory;
        this.proxyLogRepository    = proxyLogRepository;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object            result         = null;
        Throwable         throwable      = null;
        LogOperateContext operateContext = operateContextFactory.create(invocation);
        try {
            result = operateContext.execute();
        } catch (Throwable error) {
            log.info("log operate intercept business error:", error);
            throwable = error;
        }
        try {
            LogRecord logRecord = operateContext.logRecord(application);
            if (logRecord != null) {
                proxyLogRepository.saveRecord(logRecord);
            }
        } catch (Exception e) {
            log.error("save the operate log error:", e);
        }
        if (throwable != null) {
            throw throwable;
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.application = this.environment.getProperty("spring.application.name");
        if (!StringUtils.hasText(application)) {
            throw new IllegalArgumentException("application name be null. please set spring.application.name...");
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
