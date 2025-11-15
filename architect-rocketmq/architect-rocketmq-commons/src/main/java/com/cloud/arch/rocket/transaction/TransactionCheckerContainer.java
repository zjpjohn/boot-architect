package com.cloud.arch.rocket.transaction;

import com.cloud.arch.rocket.annotations.TxChecker;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.util.Optional;

public class TransactionCheckerContainer
        implements SmartInitializingSingleton, ApplicationContextAware, EmbeddedValueResolverAware {
    private final Table<String, String, TransactionChecker> checkerTable = HashBasedTable.create();
    private       ApplicationContext                        context;
    private       StringValueResolver                       resolver;

    public TransactionState checkTransaction(String topic, String tag, String key) {
        return Optional.ofNullable(checkerTable.get(topic, tag))
                       .map(checker -> checker.checkTransaction(topic, tag, key))
                       .orElse(TransactionState.UNKNOWN);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = context;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void afterSingletonsInstantiated() {
        context.getBeansOfType(TransactionChecker.class).values().forEach(this::parseTransactionChecker);
    }

    private void parseTransactionChecker(TransactionChecker checker) {
        TxChecker anno = checker.getClass().getAnnotation(TxChecker.class);
        if (anno != null) {
            String topic = resolver.resolveStringValue(anno.topic());
            String tag   = resolver.resolveStringValue(anno.tag());
            checkerTable.put(topic, tag, checker);
        }
    }

}
