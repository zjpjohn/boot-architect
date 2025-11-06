package com.cloud.arch.web.configurer;

import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.interceptor.UriResourceAuthorizeInterceptor;
import com.cloud.arch.web.support.UriAuthorityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Slf4j
public class AuthorityInterceptorConfigurer implements WebMvcConfigurer {

    private final UriAuthorityManager             uriAuthorityManager;
    private final UriResourceAuthorizeInterceptor authorizeInterceptor;

    public AuthorityInterceptorConfigurer(UriAuthorityManager uriAuthorityManager,
                                          UriResourceAuthorizeInterceptor authorizeInterceptor) {
        this.uriAuthorityManager  = uriAuthorityManager;
        this.authorizeInterceptor = authorizeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> patternList = uriAuthorityManager.getPatternList();
        if (CollectionUtils.isEmpty(patternList)) {
            log.info("no uri resources authorities has be configured, authority interceptor not registry.");
            return;
        }
        log.info("uri resources authorities has be configured, registry authority interceptor.");
        registry.addInterceptor(authorizeInterceptor)
                .addPathPatterns(patternList)
                .excludePathPatterns(this.uriAuthorityManager.getExcudeList());
    }

}
