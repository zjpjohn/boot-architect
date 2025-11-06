package com.cloud.arch.event.storage;


import com.cloud.arch.event.core.publish.Version;
import lombok.Getter;

@Getter
public class ConcurrentVersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1240869482303738118L;

    private final Version version;

    public ConcurrentVersionConflictException(String message, Version version) {
        super(message);
        this.version = version;
    }

}
