package com.cloud.arch.cache.support;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public class SimpleKey {

    public static final SimpleKey EMPTY = new SimpleKey();

    @Getter
    private Object[] params;
    private int      hashcode;

    public SimpleKey() {
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
        return (this == obj || (obj instanceof SimpleKey
                && Arrays.deepEquals(this.params, ((SimpleKey) obj).params)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
    }

}
