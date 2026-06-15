package com.cloud.arch.web.advice.handler;

import com.cloud.arch.web.domain.ApiReturn;
import com.cloud.arch.web.error.ApiBizException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;


/**
 * WebMVC 异常统一处理，覆盖参数校验、业务异常、HTTP 标准错误码等场景。
 */
@Slf4j
@RestControllerAdvice
public class WebmvcHandlerAdvice implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 处理参数为对象校验异常
     *
     * @param exception {@link BindException}
     */
    @ExceptionHandler(value = BindException.class)
    public ApiReturn<String> exception(BindException exception) {
        String     message;
        FieldError fieldError = exception.getBindingResult().getFieldError();
        if (fieldError == null) {
            message = "param validate error";
        } else if (fieldError.contains(Throwable.class)) {
            Throwable unwrapped = fieldError.unwrap(Throwable.class);
            Throwable cause     = ExceptionUtils.getRootCause(unwrapped);
            message = StringUtils.defaultString(cause.getMessage());
        } else {
            message = fieldError.getDefaultMessage();
        }
        return ApiReturn.badRequest(message, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 方法上的参数验证
     *
     * @param exception {@link ConstraintViolationException}
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ApiReturn<String> exception(ConstraintViolationException exception) {
        if (log.isErrorEnabled()) {
            log.error(exception.getMessage(), exception);
        }
        return ApiReturn.badRequest(exception.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 请求参数错误处理
     */
    @ExceptionHandler(value = {HttpMessageConversionException.class,
            InvalidPropertyException.class,
            ServletRequestBindingException.class,
            IllegalArgumentException.class})
    public ApiReturn<String> error(Exception error) {
        if (log.isErrorEnabled()) {
            log.error(error.getMessage(), error);
        }
        Throwable cause   = ExceptionUtils.getRootCause(error);
        String    message = StringUtils.defaultString(cause.getMessage());
        return ApiReturn.badRequest(message);
    }

    /**
     * 空指针异常拦截
     */
    @ExceptionHandler(value = NullPointerException.class)
    public ApiReturn<String> nullPointer(NullPointerException error) {
        log.error(error.getMessage(), error);
        return ApiReturn.serverError("Internal server error.");
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(value = ApiBizException.class)
    public ApiReturn<?> bizError(ApiBizException exception) {
        return new ApiReturn<>(exception.getStatus(), exception.getCode(), null, exception.getError(), exception.getData());
    }

    /**
     * 405-请求方法不允许
     */
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ApiReturn<String> methodError(Exception exception) {
        return new ApiReturn<>(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.value(), exception.getMessage());
    }

    /**
     * 数据库唯一键重复
     */
    @ExceptionHandler(value = DuplicateKeyException.class)
    public ApiReturn<String> duplicateError(DuplicateKeyException error) {
        if (log.isErrorEnabled()) {
            log.error(error.getMessage(), error);
        }
        return ApiReturn.badRequest(error.getProblem(), 400);
    }

    /**
     * 处理415错误
     */
    @ExceptionHandler(value = {HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class})
    public ApiReturn<String> mediaTypeError(Exception error) {
        if (log.isWarnEnabled()) {
            log.warn(error.getMessage(), error);
        }
        return ApiReturn.unsupportedMedia(error.getMessage());
    }

    /**
     * 处理404错误
     */
    @ExceptionHandler(value = {NoHandlerFoundException.class, NoResourceFoundException.class})
    public ApiReturn<String> notFoundError(Exception error) {
        return ApiReturn.notFound(HttpStatus.NOT_FOUND.value(), "your request not found.");
    }

}
