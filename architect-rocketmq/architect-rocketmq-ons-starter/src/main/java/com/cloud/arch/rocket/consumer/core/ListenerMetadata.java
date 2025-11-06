package com.cloud.arch.rocket.consumer.core;

import com.aliyun.openservices.ons.api.Message;
import com.cloud.arch.rocket.annotations.Listener;
import com.cloud.arch.rocket.annotations.Payload;
import com.cloud.arch.rocket.domain.MessageModel;
import com.cloud.arch.rocket.idempotent.Idempotent;
import com.cloud.arch.rocket.idempotent.IdempotentChecker;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.utils.AnnotationUtils;
import com.cloud.arch.rocket.utils.RocketOnsConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


@Slf4j
public class ListenerMetadata {

    private final StringValueResolver resolver;
    private final Object              bean;
    private final Method              method;
    private final Listener            listener;
    private final String              group;
    private final MessageModel        model;
    private final IdempotentChecker   idempotentChecker;
    private       String              topic;
    private       String              tag;
    private       Integer             payloadIndex;
    private       Integer             msgIndex;
    private       Class<?>[]          types;

    public ListenerMetadata(String group,
                            MessageModel model,
                            Object bean,
                            Method method,
                            Listener listener,
                            IdempotentChecker idempotentChecker,
                            StringValueResolver resolver) {
        this.group             = group;
        this.model             = model;
        this.bean              = bean;
        this.method            = method;
        this.listener          = listener;
        this.resolver          = resolver;
        this.idempotentChecker = idempotentChecker;
        this.initialize();
    }

    /**
     * 初始化参数校验并标注参数位置
     */
    private void initialize() {
        //消息topic
        this.topic = resolver.resolveStringValue(listener.topic());
        Assert.state(StringUtils.isNotBlank(topic), "请配置消息消费topic.");

        //消息tag解析校验,配置规则:不允许为空，不允许为'*',不允许包含'||'
        this.tag = resolver.resolveStringValue(listener.tag());
        boolean tagValidated = StringUtils.isNotBlank(tag)
                               && !RocketOnsConstants.ONS_ALL_TAG_REGEX.equals(this.tag)
                               && !this.tag.contains(RocketOnsConstants.ONS_COMPOSITE_TAG_DELIMITER);
        Assert.state(tagValidated, "请配置具有业务意义的消息tag.");

        //参数个数校验
        this.types = method.getParameterTypes();
        Assert.state(types.length >= 1 && types.length <= 2, "消息消费者方法参数错误.");
        //消息体参数校验
        Annotation[][] annotations  = method.getParameterAnnotations();
        Integer        payloadIndex = AnnotationUtils.annotationIndex(annotations, Payload.class);
        payloadIndex = payloadIndex != null ? payloadIndex : 0;
        if (!(ClassUtils.isAssignableValue(Serializable.class, types[payloadIndex])
              && !ClassUtils.isAssignableValue(Message.class, types[payloadIndex]))) {
            throw new IllegalArgumentException("消息内容参数必须实现Serializable接口，且不能为Message类.");
        }
        this.payloadIndex = payloadIndex;
        //原始消息参数校验
        if (types.length == 2 && ClassUtils.isAssignable(Message.class, types[1 - payloadIndex])) {
            this.msgIndex = 1 - payloadIndex;
        }
    }

    /**
     * 消息业务处理
     *
     * @param message   消息内容
     * @param serialize 序列化
     */
    public void invoke(Message message, Serialize serialize) throws Exception {
        this.method.invoke(bean, arguments(message, serialize));
    }

    /**
     * 获取@Listener方法参数
     *
     * @param message   ons原始消息
     * @param serialize 消息内容序列化
     */
    public Object[] arguments(Message message, Serialize serialize) {
        Object payload = serialize.deSerialize(message.getBody(), types[payloadIndex]);
        if (types.length == 1) {
            return new Object[]{payload};
        }
        Object[] args = new Object[2];
        args[payloadIndex] = payload;
        args[msgIndex]     = message;
        return args;
    }

    public String getGroup() {
        return group;
    }

    public MessageModel getModel() {
        return model;
    }

    public IdempotentChecker getIdempotentChecker() {
        return idempotentChecker;
    }

    /**
     * 解析消息topic
     */
    public String topic() {
        return topic;
    }

    /**
     * 解析消息过滤tag
     */
    public String tag() {
        return tag;
    }

    public Boolean idempotent() {
        return listener.idempotent() != Idempotent.NONE;
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

    public Listener getListener() {
        return listener;
    }
}
