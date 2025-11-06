package com.cloud.arch.web.custom;

import com.cloud.arch.web.advice.UniformResponseBodyAdvice;
import com.cloud.arch.web.props.WebmvcProperties;
import com.google.common.collect.Lists;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

public record CustomWebMvcRegistrations(UniformResponseBodyAdvice responseBodyAdvice, WebmvcProperties properties)
        implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        if (properties.getVersion().isEnable()) {
            return new VersionRequestHandlerMapping(properties.getVersion());
        }
        return null;
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        RequestMappingHandlerAdapter handlerAdapter      = new RequestMappingHandlerAdapter();
        List<ResponseBodyAdvice<?>>  responseBodyAdvices = Lists.newArrayList(responseBodyAdvice);
        handlerAdapter.setResponseBodyAdvice(responseBodyAdvices);
        return handlerAdapter;
    }

    @Override
    public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
        ExceptionHandlerExceptionResolver exceptionResolver = new ExceptionHandlerExceptionResolver();
        exceptionResolver.setResponseBodyAdvice(Lists.newArrayList(responseBodyAdvice));
        return exceptionResolver;
    }

}
