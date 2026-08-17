package com.cloud.arch.operate.core;

import com.cloud.arch.Ip2RegionSearcher;
import com.cloud.arch.IpRegionResult;
import com.cloud.arch.operate.props.OperateLogProperties;
import com.cloud.arch.trigger.ConsumerListener;
import com.cloud.arch.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

@Slf4j
public class OperationLogHandle implements ConsumerListener<LogContext>, EnvironmentAware {

    private static final String APP_NAME_KEY = "spring.application.name";

    private Environment environment;

    private final ILogStorage          repository;
    private final OperateLogProperties properties;
    private final Ip2RegionSearcher    ipRegionSearcher;
    private final IOperatorResolver    operatorResolver;
    private final ITenantResolver      tenantResolver;

    public OperationLogHandle(ILogStorage repository, OperateLogProperties properties, Ip2RegionSearcher ipRegionSearcher, IOperatorResolver operatorResolver, ITenantResolver tenantResolver) {
        this.repository = repository;
        this.properties = properties;
        this.ipRegionSearcher = ipRegionSearcher;
        this.operatorResolver = operatorResolver;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public void handle(List<LogContext> contexts) {
        try {
            List<Long>         idList   = CollectionUtils.toList(contexts, e -> e.getContext().getOperatorId());
            Map<Long, String>  operator = operatorResolver.resolve(idList);
            String             tenantId = tenantResolver.resolve();
            List<OperationLog> logList  = contexts.stream().map(context -> build(context, operator, tenantId)).toList();
            this.repository.save(logList);
        } catch (Exception error) {
            log.error("async save the operate logs error:", error);
        }
    }

    private OperationLog build(LogContext context, Map<Long, String> operator, String tenantId) {
        String       appNo    = environment.getProperty(APP_NAME_KEY);
        List<String> excludes = properties.excludeList();
        return context.buildLog(appNo, tenantId, excludes, operator::get, this::locationResolve);
    }

    private String locationResolve(String ip) {
        return ipRegionSearcher.searchOpt(ip).map(IpRegionResult::getAddress).orElse("");
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
