package com.cloud.arch.web.context;

import com.cloud.arch.web.context.impl.FormRequestHandler;
import com.cloud.arch.web.context.impl.JsonRequestHandler;
import com.cloud.arch.web.context.impl.GenericRequestHandler;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public enum RequestEnum {
    GENERIC {
        @Override
        public AbsRequestHandler build(RequestContext context) {
            return new GenericRequestHandler(context);
        }
    },
    FORM {
        @Override
        public AbsRequestHandler build(RequestContext context) {
            return new FormRequestHandler(context);
        }
    },
    JSON {
        @Override
        public AbsRequestHandler build(RequestContext context) {
            return new JsonRequestHandler(context);
        }
    };

    public abstract AbsRequestHandler build(RequestContext context);

    public static RequestEnum valueOf(HttpMethod method, MediaType mediaType) {
        //表单请求重写参数
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
            return FORM;
        }
        //json请求重写参数
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            return JSON;
        }
        //其他请求方式直接重写参数
        return GENERIC;
    }
}
