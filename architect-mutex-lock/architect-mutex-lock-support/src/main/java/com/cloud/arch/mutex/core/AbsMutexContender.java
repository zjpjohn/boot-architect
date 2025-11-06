package com.cloud.arch.mutex.core;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbsMutexContender implements MutexContender {

    private final String mutex;
    private final String contenderId;

    public AbsMutexContender(String mutex) {
        Preconditions.checkArgument(StringUtils.isNotBlank(mutex), "mutex name must not be null.");
        this.mutex       = mutex;
        this.contenderId = ContenderIdGenerator.HOST.generate();
    }

    /**
     * 竞争资源标识
     */
    @Override
    public String getMutex() {
        return this.mutex;
    }

    /**
     * 竞争者标识
     */
    @Override
    public String getContenderId() {
        return this.contenderId;
    }

}
