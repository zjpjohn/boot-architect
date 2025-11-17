package com.cloud.arch.rocket.consumer.core;

import com.cloud.arch.rocket.annotations.Listener;
import com.cloud.arch.rocket.annotations.Payload;
import com.cloud.arch.rocket.domain.MessageModel;
import com.cloud.arch.rocket.idempotent.Idempotent;
import com.cloud.arch.rocket.idempotent.IdempotentChecker;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.utils.AnnotationUtils;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

@Getter
public class ListenerMetadata {

    private final StringValueResolver resolver;
    private final Object              bean;
    private final Method              method;
    private final Listener            listener;
    private final IdempotentChecker   idempotentChecker;
    private final String              group;
    private       MessageModel        model;
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
                            StringValueResolver resolver,
                            IdempotentChecker idempotentChecker) {
        this.group             = group;
        this.model             = model;
        this.resolver          = resolver;
        this.bean              = bean;
        this.method            = method;
        this.listener          = listener;
        this.idempotentChecker = idempotentChecker;
        this.initialize();
    }

    private void initialize() {
        //订阅消息topic
        this.topic = resolver.resolveStringValue(listener.topic());
        Assert.state(StringUtils.isNotBlank(this.topic), "消费监听topic不允许为空.");

        //消息过滤tag,配置规则:不允许为空，不允许为'*',不允许包含'||'
        this.tag = resolver.resolveStringValue(listener.tag());
        Assert.state(RocketmqUtils.isValidTag(tag), "请配置具有业务意义的消息tag.");

        //参数个数校验
        this.types = method.getParameterTypes();
        Assert.state(types.length >= 1 && types.length <= 2, "消息消费者方法参数错误.");

        //消息体参数校验
        Annotation[][] annotations  = method.getParameterAnnotations();
        Integer        payloadIndex = AnnotationUtils.annotationIndex(annotations, Payload.class);
        //如果没有使用@Payload注解，则默认第一个参数为消息体参数
        payloadIndex = payloadIndex != null ? payloadIndex : 0;
        if (!(ClassUtils.isAssignableValue(Serializable.class, types[payloadIndex]) && !ClassUtils.isAssignableValue(
                MessageExt.class,
                types[payloadIndex]))) {
            throw new IllegalArgumentException("消息内容参数必须实现Serializable接口，且不能为MessageExt类.");
        }
        this.payloadIndex = payloadIndex;
        //原始消息参数校验
        if (types.length == 2 && ClassUtils.isAssignable(MessageExt.class, types[1 - payloadIndex])) {
            this.msgIndex = 1 - payloadIndex;
        }

    }

    /**
     * 调用方法
     *
     * @param message   原始消息内容
     * @param serialize 消息内容序列化
     * @throws Exception
     */
    public Object invoke(MessageExt message, Serialize serialize) throws Exception {
        return this.method.invoke(bean, arguments(message, serialize));
    }

    /**
     * 构建@Listener方法参数
     *
     * @param message   原始消息内容
     * @param serialize 消息内容序列化
     */
    public Object[] arguments(MessageExt message, Serialize serialize) {
        Object payload = serialize.deSerialize(message.getBody(), types[payloadIndex]);
        if (types.length == 1) {
            return new Object[]{payload};
        }
        Object[] args = new Object[2];
        args[payloadIndex] = payload;
        args[msgIndex]     = message;
        return args;
    }

    public Boolean idempotent() {
        return listener.idempotent() != Idempotent.NONE && idempotentChecker != null;
    }

    /**
     * 抽取消息幂等标识
     *
     * @param message 消息队列原始消息
     */
    public Pair<String, Integer> extractIdempotent(MessageExt message) {
        return Optional.ofNullable(message.getKeys())
                       .filter(StringUtils::isNotBlank)
                       .map(v -> Pair.of(v, 1))
                       .orElseGet(() -> Pair.of(message.getMsgId(), 0));
    }


}
