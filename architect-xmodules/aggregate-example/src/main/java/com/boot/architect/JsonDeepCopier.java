package com.boot.architect;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.aggregate.DeepCopier;
import com.cloud.arch.aggregate.Entity;
import com.google.common.base.Preconditions;

import java.io.Serializable;

public class JsonDeepCopier implements DeepCopier {

    /**
     * 对象深拷贝
     *
     * @param source 待拷贝对象
     */
    @Override
    public <T extends Entity<? extends Serializable>> T copy(T source) {
        Preconditions.checkNotNull(source, "copy object must not be null.");
        return JSON.copy(source);
    }

}
