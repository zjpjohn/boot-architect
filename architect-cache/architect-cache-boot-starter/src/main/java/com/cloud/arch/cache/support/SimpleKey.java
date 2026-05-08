package com.cloud.arch.cache.support;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 简单缓存 Key 实现，基于方法参数数组的 deepHashCode/deepEquals 生成不可变 Key，
 * 受 {@link SimpleKeyGenerator} 调用。单参返回参数本身，多参返回 SimpleKey 实例。
 */
public class SimpleKey {

    public static final SimpleKey EMPTY = new SimpleKey();

    @Getter
    private final Object[] params;
    private final int      hashcode;

    public SimpleKey() {
        this.params = new Object[0];
        this.hashcode = Arrays.deepHashCode(this.params);
    }

    public SimpleKey(Object... params) {
        this.params = new Object[params.length];
        System.arraycopy(params, 0, this.params, 0, params.length);
        this.hashcode = Arrays.deepHashCode(this.params);
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }


    @Override
    public boolean equals(Object obj) {
        return (this == obj || (obj instanceof SimpleKey && Arrays.deepEquals(this.params, ((SimpleKey) obj).params)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
    }

}
