package com.cloud.arch.operate.core;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.operate.annotations.OpLog;
import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.utils.RequestUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Getter
public class LogContext {

    private final Object         target;
    private final Method         method;
    private final OpLog          annotation;
    private final Class<?>       targetClass;
    private final String         error;
    private final Long           takenTime;
    private final Integer        state;
    private final RequestContext context;

    public LogContext(ProceedingJoinPoint joinPoint, Long takenTime, Throwable throwable) {
        this.method      = ((MethodSignature) joinPoint.getSignature()).getMethod();
        this.annotation  = method.getAnnotation(OpLog.class);
        this.target      = joinPoint.getTarget();
        this.targetClass = this.target.getClass();
        this.context     = new RequestContext();
        this.takenTime   = takenTime;
        this.state       = throwable != null ? 1 : 0;
        this.error       = errorMessage(throwable);
    }

    private String errorMessage(Throwable throwable) {
        return Optional.ofNullable(throwable).map(ExceptionUtils::getMessage).orElse("");
    }

    public OperationLog buildLog(String appNo,
                                 String tenantId,
                                 List<String> excludes,
                                 Function<Long, String> operator,
                                 Function<String, String> ipSearcher) {
        OperationLog log = new OperationLog();
        log.setAppNo(appNo);
        log.setState(state);
        log.setError(error);
        log.setTenantId(tenantId);
        log.setTakenTime(takenTime);
        log.setType(annotation.type());
        log.setTitle(annotation.title());
        log.setTarget(this.targetMethod());
        log.setBizGroup(annotation.bizGroup());
        log.setMethod(this.context.method);
        log.setReqUri(this.context.requestUri);
        log.setOpId(this.context.operatorId);
        log.setOpIp(this.context.requestIp);
        log.setParams(this.logParams(excludes));
        log.setOpName(this.operatorName(operator));
        log.setOpLocation(this.location(ipSearcher));
        return log;
    }

    private String location(Function<String, String> ipSearcher) {
        return Optional.ofNullable(ipSearcher.apply(this.context.getRequestIp())).orElse("");
    }

    private String operatorName(Function<Long, String> operator) {
        return Optional.ofNullable(operator.apply(this.context.getOperatorId())).orElse("");
    }

    private String logParams(List<String> excludes) {
        List<String> allExcludes = Lists.newArrayList(this.annotation.excludes());
        allExcludes.addAll(excludes);
        Map<String, String> params = this.context.getParams();
        Map<String, String> result = Maps.newLinkedHashMap();
        params.entrySet()
              .stream()
              .filter(entry -> !allExcludes.contains(entry.getKey()))
              .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return JSON.toJSONString(result);
    }

    private String targetMethod() {
        return this.targetClass.getName() + "." + this.method.getName() + "()";
    }

    @Getter
    public static class RequestContext {
        private final String              method;
        private final Map<String, String> params;
        private final String              requestUri;
        private final String              requestIp;
        private final Long                operatorId;

        public RequestContext() {
            HttpServletRequest request = this.request();
            this.method     = request.getMethod();
            this.params     = this.paramsMap(request);
            this.requestUri = request.getRequestURI();
            this.operatorId = this.operateId(request);
            this.requestIp  = RequestUtils.ipAddress(request);
        }

        private HttpServletRequest request() {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }

        private Map<String, String> paramsMap(HttpServletRequest request) {
            Map<String, String> result = Maps.newLinkedHashMap();
            request.getParameterMap().forEach((key, value) -> result.put(key, String.join(",", value)));
            return result;
        }

        private Long operateId(HttpServletRequest request) {
            String authId = request.getHeader(WebTokenConstants.AUTH_IDENTITY_HEADER);
            return Optional.ofNullable(authId).filter(StringUtils::isNotBlank).map(Long::parseLong).orElse(null);
        }

    }
}
