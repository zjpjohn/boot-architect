package com.cloud.arch.web.filter;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.web.ITokenVerifier;
import com.cloud.arch.web.TokenAuthProperties;
import com.cloud.arch.web.VerifyResult;
import com.cloud.arch.web.WebTokenConstants;
import com.cloud.arch.web.domain.ApiReturn;
import com.cloud.arch.web.error.ApiBizException;
import com.cloud.arch.web.props.WebShareProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Slf4j
public class JwtAuthTokenFilter extends OncePerRequestFilter {

    public static final String AUTH_FILTER_NAME = "jwtAuthTokenFilter";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String>   excludes    = Lists.newArrayList();

    private final ITokenVerifier tokenVerifier;

    public JwtAuthTokenFilter(ITokenVerifier tokenVerifier,
                              TokenAuthProperties properties,
                              ObjectProvider<WebShareProperties> shareConfig) {
        this.tokenVerifier = tokenVerifier;
        this.excludesParse(properties, shareConfig);
    }

    /**
     * 排除路径接卸
     */
    private void excludesParse(TokenAuthProperties properties, ObjectProvider<WebShareProperties> shareConfig) {
        Set<String> result = Sets.newHashSet();
        shareConfig.ifAvailable(prop -> result.addAll(prop.excludes()));
        result.addAll(properties.excludes());
        excludes.addAll(result);
    }

    /**
     * 过滤exclusions中的uri(支持正则表达式)
     */
    private boolean isExcludeUri(String uri) {
        return excludes.stream().anyMatch(exclusion -> pathMatcher.match(exclusion, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        //过滤exclusions中的请求uri
        if (this.isExcludeUri(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader(tokenVerifier.header());
        if (StringUtils.isBlank(token)) {
            writeError(response, ApiReturn.unauthorized(WebTokenConstants.UNAUTHORIZED_MESSAGE));
            return;
        }
        try {
            //token校验
            VerifyResult               verify  = tokenVerifier.verify(token);
            ServletRequestParamWrapper wrapper = new ServletRequestParamWrapper(request);
            //剔除参数中的保留字段
            wrapper.retainParameters(verify.getRetains());
            //追加token中的请求参数
            wrapper.putParameters(verify.getParameters());
            //追加header参数
            wrapper.putHeaders(verify.getHeaders());
            //包装请求修改参数以及header
            chain.doFilter(wrapper, response);
        } catch (ApiBizException error) {
            writeError(response, ApiReturn.unauthorized(error.getError()));
        } catch (Exception error) {
            writeError(response, ApiReturn.unauthorized(WebTokenConstants.LOGIN_ERROR_MESSAGE));
        }
    }

    /**
     * 返回错误响应信息
     *
     * @param response 请求响应
     * @param result   返回结果
     */
    private void writeError(HttpServletResponse response, ApiReturn<?> result) throws IOException {
        response.setStatus(result.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter writer = response.getWriter();
        writer.write(JSON.toJSONString(result.getBody()));
        writer.flush();
    }

}
