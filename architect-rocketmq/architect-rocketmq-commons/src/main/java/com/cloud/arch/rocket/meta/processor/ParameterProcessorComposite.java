package com.cloud.arch.rocket.meta.processor;


import com.cloud.arch.rocket.annotations.Delay;
import com.cloud.arch.rocket.annotations.Key;
import com.cloud.arch.rocket.annotations.Payload;
import com.cloud.arch.rocket.annotations.ShardingKey;
import com.cloud.arch.rocket.meta.processor.impl.*;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class ParameterProcessorComposite {

    private static final CallbackParameterProcessor                                 callbackProcessor = new CallbackParameterProcessor();
    private static final BodyParameterProcessor                                     bodyProcessor     = new BodyParameterProcessor();
    private static final Map<Class<? extends Annotation>, MethodParameterProcessor> processors        = new HashMap<>(4);

    static {
        processors.put(Key.class, new KeyAnnotationParameterProcessor());
        processors.put(Delay.class, new DelayAnnotationParameterProcessor());
        processors.put(Payload.class, new PayloadAnnotationParameterProcessor());
        processors.put(ShardingKey.class, new ShardingKeyAnnotationParameterProcessor());
    }

    /**
     * 获取消息体参数解析器
     */
    public static BodyParameterProcessor getBodyProcessor() {
        return bodyProcessor;
    }

    /**
     * 获取回调参数参数解析器
     */
    public static CallbackParameterProcessor getCallbackProcessor() {
        return callbackProcessor;
    }

    /**
     * 获取注解参数解析器
     *
     * @param annotation 注解类型
     */
    public static MethodParameterProcessor getAnnotationProcessor(Class<? extends Annotation> annotation) {
        return processors.get(annotation);
    }
}
