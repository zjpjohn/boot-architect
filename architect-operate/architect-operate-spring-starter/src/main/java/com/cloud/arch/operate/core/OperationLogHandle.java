package com.cloud.arch.operate.core;

import com.cloud.arch.Ip2RegionSearcher;
import com.cloud.arch.IpRegionResult;
import com.cloud.arch.operate.props.OperateLogProperties;
import com.cloud.arch.operate.repository.LogJdbcRepository;
import com.cloud.arch.trigger.ConsumerListener;
import com.cloud.arch.utils.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class OperationLogHandle implements ConsumerListener<LogContext>, EnvironmentAware {

    private static final String APP_NAME_KEY = "spring.application.name";

    private Environment environment;

    private final LogJdbcRepository    repository;
    private final OperateLogProperties properties;
    private final Ip2RegionSearcher    ipRegionSearcher;
    private final IOperatorResolver    operatorResolver;
    private final ITenantResolver      tenantResolver;

    public OperationLogHandle(LogJdbcRepository repository,
                              OperateLogProperties properties,
                              Ip2RegionSearcher ipRegionSearcher,
                              IOperatorResolver operatorResolver,
                              ITenantResolver tenantResolver) {
        this.repository       = repository;
        this.properties       = properties;
        this.ipRegionSearcher = ipRegionSearcher;
        this.operatorResolver = operatorResolver;
        this.tenantResolver   = tenantResolver;
    }

    @Override
    public void handle(List<LogContext> contexts) {
        try {
            String            appNo    = environment.getProperty(APP_NAME_KEY);
            List<Long>        idList   = CollectionUtils.toList(contexts, e -> e.getContext().getOperatorId());
            Map<Long, String> operator = operatorResolver.resolve(idList);
            List<String>      excludes = properties.excludeList();
            List<OperationLog> logList = contexts.stream()
                                                 .map(context -> build(context,
                                                                       appNo,
                                                                       tenantResolver.resolve(),
                                                                       operator,
                                                                       excludes))
                                                 .toList();
            this.repository.save(logList);
        } catch (Exception error) {
            log.error("async save the operate logs error:", error);
        }
    }

    private OperationLog build(LogContext context,
                               String appNo,
                               String tenantId,
                               Map<Long, String> operator,
                               List<String> excludes) {
        return context.buildLog(appNo, tenantId, excludes, operator::get, this::locationResolve);
    }

    private String locationResolve(String ip) {
        return Optional.ofNullable(ip).map(ipRegionSearcher::search).map(IpRegionResult::getAddress).orElse("");
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
