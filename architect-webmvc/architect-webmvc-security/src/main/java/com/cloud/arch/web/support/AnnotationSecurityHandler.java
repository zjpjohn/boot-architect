package com.cloud.arch.web.support;

import com.cloud.arch.web.annotation.Permission;
import com.cloud.arch.web.support.metadata.AuthorizationMetadata;
import com.cloud.arch.web.support.metadata.AuthorizationMetadataFactory;
import com.cloud.arch.web.utils.Assert;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;


@Slf4j
public class AnnotationSecurityHandler implements MethodInterceptor {

    private final AuthorizationMetadataFactory metaDataFactory = new AuthorizationMetadataFactory();
    private final SecurityPrincipalProcessor   securityPrincipalProcessor;

    public AnnotationSecurityHandler(SecurityPrincipalProcessor securityPrincipalProcessor) {
        this.securityPrincipalProcessor = securityPrincipalProcessor;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return this.authorizedInvoke(invocation);
    }

    /**
     * 接口授权权限校验
     */
    private Object authorizedInvoke(MethodInvocation invocation) throws Throwable {
        AuthorizationMetadata metaData   = metaDataFactory.getAndCreate(invocation);
        Permission            permission = metaData.getAnnotation();
        if (permission == null) {
            return invocation.proceed();
        }
        boolean processResult = securityPrincipalProcessor.annotationAuthorize(metaData);
        Assert.state(processResult, AuthorizationErrorHandler.AUTHORITY_FORBIDDEN);
        return invocation.proceed();
    }

}
