package com.boot.architect;

import com.cloud.arch.aggregate.DeepCopier;
import com.cloud.arch.aggregate.Entity;
import com.esotericsoftware.kryo.Kryo;

import java.io.Serializable;

public class KryoDeepCopier implements DeepCopier {

    private final ThreadLocal<Kryo> kryo;

    public KryoDeepCopier() {
        this.kryo = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            return kryo;
        });
    }

    /**
     * 对象深拷贝
     *
     * @param source 待拷贝对象
     */
    @Override
    public <T extends Entity<? extends Serializable>> T copy(T source) {
        return kryo.get().copy(source);
    }

}
