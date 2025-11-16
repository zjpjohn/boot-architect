package com.cloud.arch.web.interceptor;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.web.domain.ApiReturn;
import com.cloud.arch.web.support.AuthorizationErrorHandler;
import com.cloud.arch.web.support.UriSecurityProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
public class UriResourceAuthorizeInterceptor implements AsyncHandlerInterceptor {

    private final UriSecurityProcessor processor;

    public UriResourceAuthorizeInterceptor(UriSecurityProcessor processor) {
        this.processor = processor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            ApiReturn<Object> apiReturn;
            try {
                //注解权限优先级高于基于uri配置的权限
                if (processor.isAuthAnnotated(handlerMethod) || processor.authorize(request, handlerMethod)) {
                    return true;
                }
                apiReturn = ApiReturn.forbidden(AuthorizationErrorHandler.AUTHORITY_FORBIDDEN.getError());
            } catch (Exception error) {
                log.error("URI资源权限校验异常", error);
                apiReturn = ApiReturn.serverError(AuthorizationErrorHandler.AUTH_INTERNAL_ERROR.getError());
            }
            writeError(response, apiReturn);
            return false;
        }
        return true;
    }

    /**
     * 返回错误响应信息
     *
     * @param response 请求响应
     * @param result   返回结果
     */
    private void writeError(HttpServletResponse response, ApiReturn<?> result) throws IOException {
        response.setStatus(result.getStatusCode().value());
        response.setContentType("application/json; charset=utf-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter writer = response.getWriter();
        writer.write(JSON.toJSONString(result.getBody()));
        writer.flush();
    }

}
