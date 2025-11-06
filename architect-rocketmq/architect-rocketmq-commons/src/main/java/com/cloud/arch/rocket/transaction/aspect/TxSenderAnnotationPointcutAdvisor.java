package com.cloud.arch.rocket.transaction.aspect;

import com.cloud.arch.rocket.transaction.meta.TxSenderMetadataFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;

public class TxSenderAnnotationPointcutAdvisor extends StaticMethodMatcherPointcutAdvisor {

    private static final long serialVersionUID = 1312124618779272224L;

    public TxSenderAnnotationPointcutAdvisor() {
        setClassFilter(new TxSenderAnnotationClassFilter());
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return TxSenderMetadataFactory.getTxSenderMeta(targetClass, method) != null;
    }

}
