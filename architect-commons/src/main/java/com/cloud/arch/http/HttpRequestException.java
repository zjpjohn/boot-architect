package com.cloud.arch.http;

import lombok.Getter;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.core5.http.NoHttpResponseException;

import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.ConnectException;

@Getter
public class HttpRequestException extends RuntimeException {

    public static final Integer DEFAULT_ERROR     = 0;
    public static final Integer CONNECTION_ERROR  = 1;
    public static final Integer RESPONSE_ERROR    = 2;
    public static final Integer INTERRUPTED_ERROR = 3;
    public static final Integer HANDSHAKE_ERROR   = 4;
    public static final Integer HTTP_HOST_ERROR   = 5;
    private final       Integer status;

    public HttpRequestException(Throwable throwable) {
        super(throwable);
        if (throwable instanceof NoHttpResponseException) {
            this.status = RESPONSE_ERROR;
            return;
        }
        if (throwable instanceof InterruptedIOException) {
            this.status = INTERRUPTED_ERROR;
            return;
        }
        if (throwable instanceof SSLException) {
            this.status = HANDSHAKE_ERROR;
            return;
        }
        if (throwable instanceof HttpHostConnectException) {
            this.status = HTTP_HOST_ERROR;
            return;
        }
        if (throwable instanceof ConnectException) {
            this.status = CONNECTION_ERROR;
            return;
        }
        this.status = DEFAULT_ERROR;
    }

    public HttpRequestException(Integer status) {
        super();
        this.status = status;
    }

    public HttpRequestException(Integer status, String message) {
        super(message);
        this.status = status;
    }

    public HttpRequestException(Integer status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpRequestException(Integer status, Throwable cause) {
        super(cause);
        this.status = status;
    }

}
