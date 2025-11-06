package com.cloud.arch.event.core.publish;

public interface Version {
    Integer INITIAL_VERSION = 1;

    Integer getVersion();

    void setVersion(Integer version);
}
