package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.producer.core.OnsProducerTemplate;
import com.cloud.arch.rocket.transaction.meta.TxSenderMetadata;
import com.cloud.arch.rocket.transaction.meta.TxSenderMetadataFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.io.Serializable;

@Data
@Slf4j
public class OnsTransactionInterceptor implements MethodInterceptor, EmbeddedValueResolverAware {

    private final OnsProducerTemplate producerTemplate;
    private       StringValueResolver resolver;

    public OnsTransactionInterceptor(OnsProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        //获取事物消息元数据
        TxSenderMetadata senderMeta = TxSenderMetadataFactory.getTxSenderMeta(invocation);
        if (senderMeta == null) {
            return invocation.proceed();
        }
        Object[]     arguments = invocation.getArguments();
        String       topic     = senderMeta.getTopic(resolver);//消息topic
        Serializable payload   = senderMeta.getPayload(arguments);//消息内容
        String       key       = senderMeta.getKey(arguments);//消息业务key
        String       tag       = senderMeta.getTag(resolver);//消息tag

        ResultHolder holder = new ResultHolder();
        producerTemplate.sendTransaction(topic, tag, key, payload, message -> {
            try {
                holder.setResult(invocation.proceed());
            } catch (Throwable e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
        return holder.getResult();
    }

    @Data
    private static class ResultHolder {
        private Object result;
    }
}
