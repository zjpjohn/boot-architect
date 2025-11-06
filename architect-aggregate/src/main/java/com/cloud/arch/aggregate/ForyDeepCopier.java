package com.cloud.arch.aggregate;

import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Language;

import java.io.Serializable;

public class ForyDeepCopier implements DeepCopier {

    private final ThreadSafeFory fory;

    private static class CopierHolder {
        private static final ForyDeepCopier instance = new ForyDeepCopier();
    }

    public static ForyDeepCopier instance() {
        return CopierHolder.instance;
    }

    private ForyDeepCopier() {
        this.fory = Fory.builder()
                        .withLanguage(Language.JAVA)
                        .withRefCopy(false)
                        .requireClassRegistration(false)
                        .buildThreadSafeFory();
    }

    /**
     * 对象深拷贝
     *
     * @param source 待拷贝对象
     */
    @Override
    public <T extends Entity<? extends Serializable>> T copy(T source) {
        return this.fory.copy(source);
    }

}
