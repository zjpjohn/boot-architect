package com.cloud.arch.web.custom;

import com.cloud.arch.web.annotation.ApiVersion;
import com.cloud.arch.web.props.WebmvcProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class VersionRequestHandlerMapping extends RequestMappingHandlerMapping {

    private final WebmvcProperties.VersionConfig config;

    @Override
    protected RequestCondition<?> getCustomTypeCondition(@NonNull Class<?> handlerType) {
        ApiVersion version = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return buildCondition(version);
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(@NonNull Method method) {
        ApiVersion version = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return buildCondition(version);
    }

    private RequestCondition<?> buildCondition(ApiVersion version) {
        return Optional.ofNullable(version).map(ApiVersion::value).filter(VersionConditionNegotiate::isValid)
                       .map(RequestVersion::new).map(v -> new VersionRequestCondition(v, config)).orElse(null);
    }

}
