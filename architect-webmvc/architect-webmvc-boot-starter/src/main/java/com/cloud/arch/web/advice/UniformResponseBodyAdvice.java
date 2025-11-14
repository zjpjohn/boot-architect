package com.cloud.arch.web.advice;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.web.annotation.ApiBody;
import com.cloud.arch.web.domain.BodyData;
import com.cloud.arch.web.props.WebmvcProperties;
import com.cloud.arch.web.utils.WebMvcConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 统一响应体处理
 */
@Slf4j
public class UniformResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final WebmvcProperties properties;

    public UniformResponseBodyAdvice(WebmvcProperties properties) {
        this.properties = properties;
    }

    private ApiBody getAnnotation(MethodParameter returnType) {
        return Optional.ofNullable(returnType.getMethodAnnotation(ApiBody.class))
                       .orElseGet(() -> returnType.getDeclaringClass().getAnnotation(ApiBody.class));
    }

    private boolean isSseEmitter(MethodParameter returnType) {
        Class<?> type = Optional.ofNullable(returnType.getMethod()).map(Method::getReturnType).orElse(null);
        return type != null && ResponseBodyEmitter.class.isAssignableFrom(type);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        ApiBody annotation = this.getAnnotation(returnType);
        return annotation != null && !isSseEmitter(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        ApiBody     annotation  = this.getAnnotation(returnType);
        String      message     = message(annotation);
        BodyData<?> wrappedBody = wrapBody(annotation, body, message, response);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (body instanceof String) {
            return JSON.toJSONString(wrappedBody);
        }
        return wrappedBody;
    }

    /**
     * 响应提示消息
     */
    private String message(ApiBody annotation) {
        String message = annotation.message().trim();
        if (StringUtils.isNotBlank(message)) {
            return message;
        }
        return WebMvcConstants.DEFAULT_SUCCESS;
    }

    /**
     * 包装body体数据
     */
    private BodyData<?> wrapBody(ApiBody annotation, Object body, String message, ServerHttpResponse response) {
        if (annotation.encrypt()) {
            Object data = extractBodyData(body);
            return ResponseEncryptor.encrypt(properties, data, message, response);
        }
        if (body instanceof BodyData) {
            return (BodyData<?>) body;
        }
        return new BodyData<>(message, HttpStatus.OK.value(), body);
    }

    /**
     * 提取body内容数据
     */
    private Object extractBodyData(Object data) {
        if (data instanceof BodyData) {
            return ((BodyData<?>) data).data();
        }
        return data;
    }

}
