package com.cloud.arch.rocket.annotations;


import com.cloud.arch.rocket.domain.MessageModel;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Consumer {

    /**
     * 消费者群组group
     * 支持${}占位符
     */
    String group() default "";

    /**
     * 消息消费模式:集群消费，广播消费
     * 默认集群消费模式
     */
    MessageModel model() default MessageModel.CLUSTERING;

}
