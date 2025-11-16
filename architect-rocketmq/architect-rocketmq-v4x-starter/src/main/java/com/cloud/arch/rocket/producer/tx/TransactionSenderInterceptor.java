package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.transaction.meta.TxSenderMetadata;
import com.cloud.arch.rocket.transaction.meta.TxSenderMetadataFactory;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

@Slf4j
public class TransactionSenderInterceptor implements MethodInterceptor, EmbeddedValueResolverAware {

    private final TransactionProducerContainer producerContainer;
    private       StringValueResolver          resolver;

    public TransactionSenderInterceptor(TransactionProducerContainer producerContainer) {
        this.producerContainer = producerContainer;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        TxSenderMetadata metadata = TxSenderMetadataFactory.getTxSenderMeta(invocation);
        if (metadata == null) {
            return invocation.proceed();
        }
        Object[] arguments = invocation.getArguments();
        String   key       = metadata.getKey(arguments);//消息业务key
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("transaction message key must not be null or empty");
        }
        Serializable payload = metadata.getPayload(arguments);//消息内容
        String       topic   = metadata.getTopic(resolver);//消息topic
        String       tag     = metadata.getTag(resolver);//消息tag
        try {
            //加入本地执行上线文
            TransactionExecutorContext.setInvocation(invocation);
            //同步发送事物消息
            producerContainer.sendTransaction(topic, tag, key, payload, null);
            //获取当前执行结果
            return TransactionExecutorContext.getResult();
        } catch (Exception error) {
            throw new RuntimeException(error);
        } finally {
            TransactionExecutorContext.clear();
        }
    }

    @Override
    public void setEmbeddedValueResolver(@Nonnull StringValueResolver resolver) {
        this.resolver = resolver;
    }

}
