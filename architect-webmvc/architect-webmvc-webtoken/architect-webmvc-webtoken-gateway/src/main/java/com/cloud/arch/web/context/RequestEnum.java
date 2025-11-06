package com.cloud.arch.web.context;

import com.cloud.arch.web.context.impl.FormRequestHandler;
import com.cloud.arch.web.context.impl.JsonRequestHandler;
import com.cloud.arch.web.context.impl.QueryRequestHandler;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public enum RequestEnum {
    QUERY {
        @Override
        public AbsRequestHandler build(RequestContext context) {
            return new QueryRequestHandler(context);
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
        if (HttpMethod.GET == method || MediaType.TEXT_PLAIN.isCompatibleWith(mediaType)) {
            return QUERY;
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
            return FORM;
        }
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            return JSON;
        }
        return null;
    }
}
