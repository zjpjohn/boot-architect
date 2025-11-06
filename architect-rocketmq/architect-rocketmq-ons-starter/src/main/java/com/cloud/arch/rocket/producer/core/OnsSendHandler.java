package com.cloud.arch.rocket.producer.core;

import com.cloud.arch.rocket.meta.MessageSendHandler;
import com.cloud.arch.rocket.meta.SenderMetadata;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;

@Getter
public class OnsSendHandler implements MessageSendHandler {

    private final OnsProducerTemplate producer;
    private final SendModel           sendModel;
    private final SenderMetadata      metadata;

    public OnsSendHandler(Method method,
                          StringValueResolver resolver,
                          OnsProducerTemplate producer,
                          OnsRecogniseHandler recogniseHandler) {
        this.producer  = producer;
        this.metadata  = new SenderMetadata(method, resolver);
        this.sendModel = recogniseHandler.recognise(metadata.getAnnotation());
    }

    public String getTopic() {
        return metadata.getTopic();
    }

    public String getFilterTag() {
        return metadata.getTag();
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
        Assert.state(StringUtils.isNotBlank(this.getTopic()), "消息发送topic不允许为空");
        this.sendModel.sendCheck(metadata);
    }

}
