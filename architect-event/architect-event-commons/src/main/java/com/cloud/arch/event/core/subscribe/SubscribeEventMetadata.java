package com.cloud.arch.event.core.subscribe;

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@Getter
@ToString
public class SubscribeEventMetadata {

    /**
     * 订阅事件topic
     */
    private final String name;
    /**
     * 订阅事件类型
     */
    private final Class<?> type;
    /**
     * 订阅事件消费者分组
     */
    private String group = "";
    /**
     * 订阅事件消息过滤标识
     */
    private String filter = "";
    /**
     * 分库分表情况下拥有幂等的分库分表字段名称 需要订阅事件对象中存在对应的字段
     */
    private String sharding = "";
    /**
     * 自定义幂等key,徐聪订阅事件中解析spel对应字段的值
     */
    private String key = "";

    public SubscribeEventMetadata(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public SubscribeEventMetadata group(String group) {
        if (StringUtils.isNotBlank(group)) {
            this.group = group;
        }
        return this;
    }

    public SubscribeEventMetadata filter(String filter) {
        if (StringUtils.isNotBlank(filter)) {
            this.filter = filter;
        }
        return this;
    }

    public SubscribeEventMetadata sharding(String sharding) {
        if (StringUtils.isNotBlank(sharding)) {
            this.sharding = sharding;
        }
        return this;
    }

    public SubscribeEventMetadata key(String key) {
        if (StringUtils.isNotBlank(key)) {
            this.key = key;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SubscribeEventMetadata that = (SubscribeEventMetadata)o;
        return name.equals(that.name) && type.equals(that.type) && group.equals(that.group)
            && filter.equals(that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, group, filter);
    }

}
