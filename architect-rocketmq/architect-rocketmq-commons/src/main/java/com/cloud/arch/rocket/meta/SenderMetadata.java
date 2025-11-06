package com.cloud.arch.rocket.meta;

import com.cloud.arch.rocket.annotations.Sender;
import com.cloud.arch.rocket.meta.processor.MethodParameterProcessor;
import com.cloud.arch.rocket.meta.processor.ParameterProcessorComposite;
import com.cloud.arch.rocket.meta.processor.impl.BodyParameterProcessor;
import com.cloud.arch.rocket.meta.processor.impl.CallbackParameterProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SenderMetadata {

    public static final String                                 MESSAGE_ALL_TAG = "*";
    /**
     * 参数处理器集合
     */
    private final       Map<Integer, MethodParameterProcessor> processors      = new HashMap<>(4);

    /**
     * ${}占位符解析器
     */
    private final StringValueResolver resolver;
    /**
     * 发送者注解
     */
    private final Sender              annotation;
    /**
     * 消息topic
     */
    private       String              topic;
    /**
     * 消息过滤tag
     */
    private       String              tag;
    /**
     * 发送消息方法
     */
    private final Method              method;
    /**
     * 方法参数类型集合
     */
    private final Class<?>[]          types;
    /**
     * 发送消息内容参数标志位
     */
    private       Integer             payload;
    /**
     * 业务key参数标志位
     */
    private       Integer             key;
    /**
     * 顺序消息shardingKey参数标志位
     */
    private       Integer             shardingKey;
    /**
     * 异步消息回调参数标志位
     */
    private       Integer             async;
    /**
     * 延迟消息参数标志位为
     */
    private       DelayMetadata       delay;

    public SenderMetadata(Method method, StringValueResolver resolver) {
        this.resolver   = resolver;
        this.method     = method;
        this.annotation = method.getAnnotation(Sender.class);
        this.types      = method.getParameterTypes();
        this.checkAndBuild();
    }

    /**
     * 校验并设置参数
     */
    private void checkAndBuild() {
        this.topic = resolver.resolveStringValue(annotation.topic());
        Assert.state(StringUtils.isNotBlank(this.topic), "消息topic不允许为空.");
        //明确tag具有业务意义，强制设置为具体内容不允许设置(空或'*')
        this.tag = resolver.resolveStringValue(annotation.tag());
        Assert.state(StringUtils.isNotBlank(tag) && !MESSAGE_ALL_TAG.equals(this.tag), "请设置具体消息tag.");
        Annotation[][] annotations = method.getParameterAnnotations();
        Assert.state(annotations.length
                     > 0, String.format("方法%s.%s参数不允许为空", this.getDeclareClassName(), this.getMethodName()));
        for (int i = 0; i < annotations.length; i++) {
            Annotation[] annotation = annotations[i];
            if (annotation.length == 0) {
                parseParameter(types[i], i);
                continue;
            }
            parseAnnotation(annotation, types[i], i);
        }
    }

    private void parseAnnotation(Annotation[] annotations, Class<?> type, Integer index) {
        for (Annotation annotation : annotations) {
            MethodParameterProcessor annotatedProcessor
                    = ParameterProcessorComposite.getAnnotationProcessor(annotation.annotationType());
            if (annotatedProcessor != null) {
                annotatedProcessor.buildMeta(this, type, index, annotation);
                return;
            }
        }
    }

    private void parseParameter(Class<?> type, Integer index) {
        //处理异步消息回调接口，必须为MsgSendCallback接口实现类
        if (annotation.async() && ClassUtils.isAssignable(MsgSendCallback.class, this.types[index])) {
            CallbackParameterProcessor processor = ParameterProcessorComposite.getCallbackProcessor();
            processor.buildMeta(this, type, index);
            return;
        }
        //处理未标记参数为消息体，必须为Serializable接口
        if (ClassUtils.isAssignable(Serializable.class, type)) {
            BodyParameterProcessor processor = ParameterProcessorComposite.getBodyProcessor();
            processor.buildMeta(this, type, index);
        }
    }

    public String getTopic() {
        return topic;
    }

    public String getTag() {
        return tag;
    }

    public Sender getAnnotation() {
        return annotation;
    }

    public Method getMethod() {
        return method;
    }

    public Integer getTimeout() {
        return annotation.timeout();
    }

    public Integer getBatchSize() {
        return annotation.batchSize();
    }

    public Class<?>[] getTypes() {
        return types;
    }

    public Integer getPayload() {
        return payload;
    }

    public Integer getKey() {
        return key;
    }

    public Integer getShardingKey() {
        return shardingKey;
    }

    public Integer getAsync() {
        return async;
    }

    public DelayMetadata getDelay() {
        return delay;
    }

    public void setDelay(DelayMetadata delay) {
        this.delay = delay;
    }

    public Map<Integer, MethodParameterProcessor> getProcessors() {
        return processors;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public void setShardingKey(Integer shardingKey) {
        this.shardingKey = shardingKey;
    }

    public void setAsync(Integer async) {
        this.async = async;
    }

    public String getDeclareClassName() {
        return this.method.getDeclaringClass().getSimpleName();
    }

    public String getMethodName() {
        return this.method.getName();
    }

    /**
     * 延迟时间参数元数据
     */
    public static class DelayMetadata {

        //延迟参数位置
        private final Integer  index;
        //是否为deliver指定时间点
        private final Boolean  deliver;
        //延迟时间单位
        private final TimeUnit timeUnit;
        //延迟时间是否为集合
        private       Boolean  collection;

        public DelayMetadata(Integer index, Boolean deliver, TimeUnit timeUnit) {
            this.index    = index;
            this.deliver  = deliver;
            this.timeUnit = timeUnit;
        }

        public Integer getIndex() {
            return index;
        }

        public Boolean isDeliver() {
            return deliver;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public void setCollection(Boolean collection) {
            this.collection = collection;
        }

        public Boolean isCollection() {
            return collection;
        }
    }
}
