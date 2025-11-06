package com.cloud.arch.rocket.transaction.meta;

import com.cloud.arch.rocket.annotations.Key;
import com.cloud.arch.rocket.annotations.Payload;
import com.cloud.arch.rocket.annotations.TxSender;
import com.cloud.arch.rocket.utils.AnnotationUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

@Getter
public class TxSenderMetadata {

    private final Class<?> targetClass;
    private final Method   method;
    private final String   topic;
    private final String   tag;
    private final Integer  payloadIndex;
    private final Integer  keyIndex;

    public TxSenderMetadata(Class<?> targetClass, Method method, TxSender txSender) {
        this.targetClass  = targetClass;
        this.method       = method;
        this.topic        = txSender.topic();
        this.tag          = txSender.tag();
        this.payloadIndex = this.payloadIndex(method);
        this.keyIndex     = keyIndex(method);
    }

    private Integer payloadIndex(Method method) {
        Annotation[][] annotations  = method.getParameterAnnotations();
        Integer        payloadIndex = AnnotationUtils.annotationIndex(annotations, Payload.class);
        if (payloadIndex == null) {
            payloadIndex = 0;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?>   payloadType    = parameterTypes[payloadIndex];
        if (ClassUtils.isAssignable(Serializable.class, payloadType)) {
            return payloadIndex;
        }
        return null;
    }

    private Integer keyIndex(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        Integer        keyIndex    = AnnotationUtils.annotationIndex(annotations, Key.class);
        if (keyIndex != null) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?>   keyType        = parameterTypes[keyIndex];
            if (ClassUtils.isAssignable(String.class, keyType)) {
                return keyIndex;
            }
        }
        return null;
    }

    public String getTopic(StringValueResolver resolver) {
        String topic = resolver.resolveStringValue(this.topic);
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("事物消息topic不允许为空");
        }
        return topic;
    }

    public String getTag(StringValueResolver resolver) {
        return resolver.resolveStringValue(this.tag);
    }

    public Serializable getPayload(Object[] args) {
        if (payloadIndex == null || args[payloadIndex] == null) {
            throw new IllegalArgumentException("事物消息内容不允许为空");
        }
        return (Serializable) args[payloadIndex];
    }

    public String getKey(Object[] args) {
        return Optional.ofNullable(keyIndex).map(key -> (String) args[key]).orElse(null);
    }

    @Override
    public String toString() {
        return "TxSenderMetadata{"
               + "targetClass="
               + targetClass.getSimpleName()
               + ", method="
               + method.getName()
               + ", topic='"
               + topic
               + '\''
               + ", tag='"
               + tag
               + '\''
               + ", payloadIndex="
               + payloadIndex
               + ", keyIndex="
               + keyIndex
               + '}';
    }
}
