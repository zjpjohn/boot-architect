package com.cloud.arch.web.custom;

import com.cloud.arch.web.error.ApiBizException;
import com.cloud.arch.web.utils.WebMvcConstants;
import com.google.common.collect.Maps;
import jakarta.servlet.RequestDispatcher;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * 自定义错误属性，统一 404 和拦截器异常的错误响应格式，返回 JSON 结构的错误数据。
 */
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
        attrs.put(WebMvcConstants.ERROR_ATTR_ERROR_KEY, status ==
                                                        HttpStatus.NOT_FOUND.value() ? WebMvcConstants.PAGE_NOT_FOUND_MESSAGE : WebMvcConstants.SERVER_ERROR_MESSAGE);
        return attrs;
    }

    private Integer getStatusCode(WebRequest request) {
        Throwable error = getError(request);
        if (error instanceof ApiBizException exception) {
            return exception.getStatus().value();
        }
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE, RequestAttributes.SCOPE_REQUEST);
        if (statusCode instanceof Integer code) {
            return code;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

}
