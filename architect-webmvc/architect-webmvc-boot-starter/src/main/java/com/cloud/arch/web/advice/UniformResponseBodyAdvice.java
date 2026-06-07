package com.cloud.arch.web.advice;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.cloud.arch.web.annotation.ApiBody;
import com.cloud.arch.web.domain.BodyData;
import com.cloud.arch.web.props.WebmvcProperties;
import com.cloud.arch.web.utils.WebMvcConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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

    private ApiBody getAnnotation(MethodParameter parameter) {
        return Optional.ofNullable(parameter.getMethodAnnotation(ApiBody.class))
                       .orElseGet(() -> parameter.getDeclaringClass().getAnnotation(ApiBody.class));
    }

    private boolean isSseEmitter(MethodParameter returnType) {
        Class<?> methodReturnType = Optional.ofNullable(returnType.getMethod()).map(Method::getReturnType).orElse(null);
        if (methodReturnType != null && ResponseBodyEmitter.class.isAssignableFrom(methodReturnType)) {
            return true;
        }
        // 异步场景：Spring 重分发时 ConcurrentResultMethodParameter.getParameterType() 返回解析后的实际类型
        // 当参数类型与方法返回类型不同，说明经历了异步解包，需要检查解析后的类型是否为 SSE
        Class<?> paramType = returnType.getParameterType();
        return paramType != methodReturnType && ResponseBodyEmitter.class.isAssignableFrom(paramType);
    }

    private boolean isStreamReturnType(MethodParameter returnType) {
        Class<?> type = returnType.getParameterType();
        return InputStreamResource.class.isAssignableFrom(type) || Resource.class.isAssignableFrom(type);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        ApiBody annotation = this.getAnnotation(returnType);
        return annotation != null && !isSseEmitter(returnType) && !isStreamReturnType(returnType);
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
        if (body == null || body instanceof String) {
            return JSON.toJSONString(wrappedBody, JSONWriter.Feature.WriteLongAsString);
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
     * 包装body数据
     */
    private BodyData<?> wrapBody(ApiBody annotation, Object body, String message, ServerHttpResponse response) {
        if (annotation.encrypt()) {
            Object data = unWrapBody(body);
            return ResponseEncryptor.encrypt(properties, data, message, response);
        }
        return wrapAdapter(body, message);
    }

    /**
     * 统一适配包装方法返回值
     */
    private BodyData<?> wrapAdapter(Object body, String message) {
        if (body instanceof BodyData) {
            return (BodyData<?>) body;
        }
        Object data = body;
        if (data instanceof Optional<?> value) {
            data = value.orElse(null);
        }
        return new BodyData<>(message, HttpStatus.OK.value(), data);
    }

    /**
     * 解构body内容数据
     */
    private Object unWrapBody(Object data) {
        if (data instanceof BodyData) {
            return ((BodyData<?>) data).data();
        }
        if (data instanceof Optional<?> value) {
            return value.orElse(null);
        }
        return data;
    }

}
