package com.cloud.arch.aggregate;

import java.io.Serializable;

public interface DeepCopier {


    /**
     * 对象深拷贝
     *
     * @param source 待拷贝对象
     */
    <T extends Entity<? extends Serializable>> T copy(T source);
}
