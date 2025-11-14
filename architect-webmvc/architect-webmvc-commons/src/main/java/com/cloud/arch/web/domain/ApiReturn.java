package com.cloud.arch.web.domain;

import com.cloud.arch.web.error.ErrorHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class ApiReturn<T> extends ResponseEntity<BodyData<T>> {

    private final BodyData<T> content;

    public ApiReturn(HttpStatusCode status, Integer code, String message, String error, T data) {
        super(status);
        this.content = new BodyData<>(error, message, code, data);
    }

    public ApiReturn(HttpStatus status, Integer code, String error) {
        this(status, code, null, error, null);
    }

    public ApiReturn(HttpStatus status, Integer code, String message, T data) {
        this(status, code, message, null, data);
    }

    @Override
    public BodyData<T> getBody() {
        return this.content;
    }

    /**
     * 200返回
     *
     * @param message 业务响应成功消息
     */
    public static <E> ApiReturn<E> success(String message) {
        return success(message, HttpStatus.OK.value());
    }

    /**
     * 200返回
     *
     * @param message 业务响应成功消息
     * @param code    响应成功业务码
     */
    public static <E> ApiReturn<E> success(String message, Integer code) {
        return new ApiReturn<>(HttpStatus.OK, code, message, null, null);
    }

    /**
     * 200返回
     *
     * @param message 业务响应成功消息
     * @param data    响应内容
     */
    public static <E> ApiReturn<E> success(String message, E data) {
        return new ApiReturn<>(HttpStatus.OK, HttpStatus.OK.value(), message, null, data);
    }

    /**
     * 基于ErrorHandler生成返回值
     */
    public static <E> ApiReturn<E> from(ErrorHandler error) {
        return new ApiReturn<>(error.getStatus(), error.getCode(), error.getError());
    }

    /**
     * 500错误
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> serverError(String error, Integer code) {
        return new ApiReturn<>(HttpStatus.INTERNAL_SERVER_ERROR, code, null, error, null);
    }

    /**
     * 500错误
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> serverError(String error) {
        return serverError(error, 500);
    }

    /**
     * 415错误
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> unsupportedMedia(String error) {
        return new ApiReturn<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, null, error, null);
    }

    /**
     * 405错误
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> methodNotAllowed(String error) {
        return new ApiReturn<>(HttpStatus.METHOD_NOT_ALLOWED, 405, null, error, null);
    }

    /**
     * 400错误
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> badRequest(String error) {
        return badRequest(error, HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 400错误
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> badRequest(String error, Integer code) {
        return new ApiReturn<>(HttpStatus.BAD_REQUEST, code, null, error, null);
    }

    /**
     * 401错误
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> unauthorized(String error, Integer code) {
        return new ApiReturn<>(HttpStatus.UNAUTHORIZED, code, null, error, null);
    }

    /**
     * 401错误
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> unauthorized(String error) {
        return unauthorized(error, HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * 403错误
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> forbidden(String error, Integer code) {
        return new ApiReturn<>(HttpStatus.FORBIDDEN, code, null, error, null);
    }

    /**
     * 403错误
     *
     * @param error 业务错误消息
     */
    public static <E> ApiReturn<E> forbidden(String error) {
        return forbidden(error, HttpStatus.FORBIDDEN.value());
    }

    /**
     * 204响应码
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> none(String error, Integer code) {
        return new ApiReturn<>(HttpStatus.NO_CONTENT, code, null, error, null);
    }

    /**
     * 204响应码
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> none(String error) {
        return none(error, HttpStatus.NO_CONTENT.value());
    }

    /**
     * 业务异常
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> bizError(String error, Integer code) {
        return new ApiReturn<>(HttpStatus.OK, code, null, error, null);
    }

    /**
     * 404错误
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> notFound(Integer code, String error) {
        return new ApiReturn<>(HttpStatus.NOT_FOUND, code, null, error, null);
    }

    /**
     * 404错误
     *
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> notFound(String error) {
        return notFound(HttpStatus.NOT_FOUND.value(), error);
    }

    /**
     * 429响应
     *
     * @param code  业务错误码
     * @param error 业务错误提示消息
     */
    public static <E> ApiReturn<E> toManyRequest(Integer code, String error) {
        return new ApiReturn<>(HttpStatus.TOO_MANY_REQUESTS, code, null, error, null);
    }

    /**
     * 429响应
     *
     * @param error 业务错误提示信息
     */
    public static <E> ApiReturn<E> toManyRequest(String error) {
        return toManyRequest(HttpStatus.TOO_MANY_REQUESTS.value(), error);
    }

}
