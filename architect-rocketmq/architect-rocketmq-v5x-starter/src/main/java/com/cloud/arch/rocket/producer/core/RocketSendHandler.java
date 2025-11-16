package com.cloud.arch.rocket.producer.core;

import com.cloud.arch.rocket.meta.MessageSendHandler;
import com.cloud.arch.rocket.meta.SenderMetadata;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;

public class RocketSendHandler implements MessageSendHandler {

    private final RocketProducerTemplate producerTemplate;
    private final SendModel           sendModel;
    private final SenderMetadata      metadata;
    private final StringValueResolver resolver;

    public RocketSendHandler(Method method,
                             RocketProducerTemplate producerTemplate,
                             StringValueResolver resolver,
                             RocketRecogniseHandler recogniseHandler) {
        this.producerTemplate = producerTemplate;
        this.resolver         = resolver;
        this.metadata         = new SenderMetadata(method, resolver);
        this.sendModel        = recogniseHandler.recognise(metadata.getAnnotation());
    }

    /**
     * 发送消息代理接口
     *
     * @param args 发送消息方法参数
     */
    @Override
    public Object invoke(Object[] args) throws Throwable {
        return this.sendModel.send(this, args);
    }

    /**
     * 校验消息发送参数校验
     */
    @Override
    public void validate() {
        Assert.state(StringUtils.isNotBlank(this.getTopic()), "消息发送topic不允许为空.");
        this.sendModel.sendCheck(metadata);
    }

    public RocketProducerTemplate getProducerTemplate() {
        return producerTemplate;
    }

    public SendModel getSendModel() {
        return sendModel;
    }

    public SenderMetadata getMetadata() {
        return metadata;
    }

    public StringValueResolver getResolver() {
        return resolver;
    }

    public String getTopic() {
        return metadata.getTopic();
    }

    public String getFilterTag() {
        return metadata.getTag();
    }

}
