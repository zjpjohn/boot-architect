package com.cloud.arch.web.custom;

import com.cloud.arch.web.error.ApiBizException;
import com.cloud.arch.web.utils.WebMvcConstants;
import com.google.common.collect.Maps;
import jakarta.servlet.RequestDispatcher;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

public class CustomErrorAttributes extends DefaultErrorAttributes {

    /**
     * 统一处理404异常和拦截器异常
     * <p/>
     * 默认错误处理返回JSON数据，构造错误数据处理格式
     * <p/>
     * 具体详见{@link BasicErrorController}
     */
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Integer             status = getStatusCode(webRequest);
        Map<String, Object> attrs  = Maps.newLinkedHashMap();
        attrs.put(WebMvcConstants.ERROR_ATTR_CODE_KEY, status);
        attrs.put(WebMvcConstants.ERROR_ATTR_TIMESTAMP_KEY, System.currentTimeMillis());
        attrs.put(WebMvcConstants.ERROR_ATTR_ERROR_KEY,
            status == HttpStatus.NOT_FOUND.value() ? WebMvcConstants.PAGE_NOT_FOUND_MESSAGE
                                                   : WebMvcConstants.SERVER_ERROR_MESSAGE);
        return attrs;
    }

    private Integer getStatusCode(WebRequest request) {
        Throwable error = getError(request);
        if (error instanceof ApiBizException exception) {
            return exception.getStatus().value();
        }
        return (Integer)request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE, RequestAttributes.SCOPE_REQUEST);
    }

}
