package com.cloud.arch.transaction.support;

import com.cloud.arch.transaction.annotation.TxAsync;
import com.cloud.arch.transaction.core.AsyncTxInvoker;
import com.cloud.arch.transaction.core.AsyncTxInvokers;
import com.cloud.arch.utils.CollectionUtils;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import java.lang.reflect.Method;
import java.util.Map;

@Getter
public class AsyncTxInvokerProcessor implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final PlatformTransactionManager transactionManager;
    private final TransactionAttributeSource transactionAttributeSource;

    public AsyncTxInvokerProcessor(PlatformTransactionManager transactionManager,
                                   TransactionAttributeSource transactionAttributeSource) {
        this.transactionManager         = transactionManager;
        this.transactionAttributeSource = transactionAttributeSource;
    }

    private void parseAsyncInvoker() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Map<Method, TxAsync> selectedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                                                                                    (MethodIntrospector.MetadataLookup<TxAsync>) method -> AnnotatedElementUtils.findMergedAnnotation(
                                                                                            method,
                                                                                            TxAsync.class));
            if (CollectionUtils.isEmpty(selectedMethods)) {
                continue;
            }
            for (Map.Entry<Method, TxAsync> entry : selectedMethods.entrySet()) {
                AsyncTxInvoker asyncTxInvoker = new AsyncTxInvoker(bean,
                                                                   entry.getKey(),
                                                                   entry.getValue(),
                                                                   transactionManager,
                                                                   transactionAttributeSource);
                AsyncTxInvokers.add(asyncTxInvoker);
            }
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.parseAsyncInvoker();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        ApplicationContextHolder.setApplicationContext(applicationContext);
    }

}
