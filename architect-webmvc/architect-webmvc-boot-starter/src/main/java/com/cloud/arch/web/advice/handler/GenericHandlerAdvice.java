package com.cloud.arch.web.advice.handler;

import com.cloud.arch.web.domain.ApiReturn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@ControllerAdvice
public class GenericHandlerAdvice implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 通用错误处理
     *
     * @param error {@link Exception}
     */
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public ApiReturn<String> exception(Exception error) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String                   requestURI        = requestAttributes.getRequest().getRequestURI();
        log.error("request '{}' handle error", requestURI, error);
        return ApiReturn.serverError("system service error.");
    }

}
