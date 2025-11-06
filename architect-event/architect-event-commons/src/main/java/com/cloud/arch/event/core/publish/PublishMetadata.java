package com.cloud.arch.event.core.publish;

import com.cloud.arch.event.annotations.Publish;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.concurrent.TimeUnit;

@Getter
public class PublishMetadata {

    /**
     * 发送事件注解
     */
    private final Publish annotation;
    /**
     * 是否为本地事件
     */
    private final boolean local;
    /**
     * 事件业务分组
     */
    private       String   bizGroup;
    /**
     * 事件topic
     */
    private       String   name;
    /**
     * 事件tag过滤
     */
    private       String   filter;
    /**
     * 延迟事件延迟时间
     */
    private       Long     delay;
    /**
     * 延迟事件延迟时间单位
     */
    private       TimeUnit timeUnit;

    public PublishMetadata(Publish annotation, StringValueResolver resolver) {
        this.annotation = annotation;
        String topic = annotation.name();
        this.local = StringUtils.isBlank(topic);
        if (!this.local) {
            this.bizGroup = annotation.bizGroup();
            this.name     = resolver.resolveStringValue(topic);
            this.filter   = resolver.resolveStringValue(annotation.filter());
            this.timeUnit = annotation.timeUnit();
            this.delay    = this.timeUnit.toMillis(annotation.delay());
        }
    }

}
