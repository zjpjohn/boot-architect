package com.cloud.arch.web.advice.handler;

import com.cloud.arch.web.domain.ApiReturn;
import com.cloud.arch.web.error.ApiBizException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
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
    @ResponseBody
    @ExceptionHandler(value = BindException.class)
    public ApiReturn<String> exception(BindException exception) {
        String message = exception.getBindingResult()
                                  .getFieldErrors()
                                  .stream()
                                  .map(FieldError::getDefaultMessage)
                                  .collect(Collectors.joining(";"));
        return ApiReturn.badRequest(message, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 方法上的参数验证
     *
     * @param exception {@link ConstraintViolationException}
     */
    @ResponseBody
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ApiReturn<String> exception(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                                  .stream()
                                  .map(ConstraintViolation::getMessage)
                                  .collect(Collectors.joining(";"));
        return ApiReturn.badRequest(message, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 请求参数错误处理
     */
    @ResponseBody
    @ExceptionHandler(
            value = {
                    MethodArgumentTypeMismatchException.class,
                    MethodArgumentConversionNotSupportedException.class,
                    HttpMessageNotReadableException.class,
                    HttpMessageNotWritableException.class,
                    ConversionException.class,
                    ConversionNotSupportedException.class,
                    InvalidPropertyException.class,
                    NumberFormatException.class,
                    ServletRequestBindingException.class,
                    UnsatisfiedServletRequestParameterException.class,
                    IllegalArgumentException.class,
            })
    public ApiReturn<String> error(Exception error) {
        String message = Optional.ofNullable(NestedExceptionUtils.getRootCause(error))
                                 .map(Throwable::getMessage)
                                 .orElse(error.getMessage());
        return ApiReturn.badRequest(message);
    }

    /**
     * 空指针异常拦截
     *
     * @param error {@link NullPointerException}
     */
    @ResponseBody
    @ExceptionHandler(value = NullPointerException.class)
    public ApiReturn<String> nullPointer(NullPointerException error) {
        log.error(error.getMessage(), error);
        return ApiReturn.serverError("internal server error.");
    }

    /**
     * 业务异常处理
     *
     * @param exception {@link ApiBizException}
     */
    @ResponseBody
    @ExceptionHandler(value = ApiBizException.class)
    public ApiReturn<?> exception(ApiBizException exception) {
        return new ApiReturn<>(exception.getStatus(),
                               exception.getCode(),
                               null,
                               exception.getError(),
                               exception.getData());
    }

    /**
     * 405-请求方法不允许
     */
    @ResponseBody
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ApiReturn<String> methodError(Exception exception) {
        return new ApiReturn<>(HttpStatus.METHOD_NOT_ALLOWED,
                               HttpStatus.METHOD_NOT_ALLOWED.value(),
                               exception.getMessage());
    }

    /**
     * 数据库唯一键重复
     *
     * @param error {@link DuplicateKeyException}
     */
    @ResponseBody
    @ExceptionHandler(value = DuplicateKeyException.class)
    public ApiReturn<String> exception(DuplicateKeyException error) {
        return ApiReturn.badRequest("has duplicated data.", 400);
    }

    @ResponseBody
    @ExceptionHandler(value = {HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class})
    public ApiReturn<String> mediaTypeError(Exception error) {
        return ApiReturn.unsupportedMedia(error.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = {NoHandlerFoundException.class, NoResourceFoundException.class})
    public ApiReturn<String> notFoundError(Exception exception) {
        return ApiReturn.notFound(HttpStatus.NOT_FOUND.value(), "your request not found.");
    }

}
