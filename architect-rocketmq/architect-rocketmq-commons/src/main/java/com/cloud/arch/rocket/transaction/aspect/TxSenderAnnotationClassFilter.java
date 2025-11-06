package com.cloud.arch.rocket.transaction.aspect;

import com.cloud.arch.rocket.annotations.Producer;
import com.cloud.arch.rocket.transaction.meta.TxSenderMetadataFactory;
import org.springframework.aop.support.annotation.AnnotationClassFilter;


public class TxSenderAnnotationClassFilter extends AnnotationClassFilter {

    public TxSenderAnnotationClassFilter() {
        super(Producer.class, true);
    }

    @Override
    public boolean matches(Class<?> clazz) {
        return super.matches(clazz) && TxSenderMetadataFactory.hasTxSenderAnnotation(clazz);
    }

}
