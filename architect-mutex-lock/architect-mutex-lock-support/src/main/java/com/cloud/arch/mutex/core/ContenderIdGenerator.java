package com.cloud.arch.mutex.core;


import com.cloud.arch.mutex.utils.Systems;
import com.google.common.base.Strings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

public enum ContenderIdGenerator {
    UUID() {
        @Override
        public String generate() {
            return java.util.UUID.randomUUID().toString().replaceAll("-", "");
        }
    },
    HOST() {

        final AtomicLong counter = new AtomicLong();

        @Override
        public String generate() {
            try {
                final InetAddress localHost = InetAddress.getLocalHost();
                final long        processId = Systems.currentProcessId();
                final long        seq       = counter.getAndIncrement();
                return Strings.lenientFormat("%s:%s@%s", seq, processId, localHost.getHostAddress());
            } catch (UnknownHostException error) {
                throw new RuntimeException(error.getMessage(), error);
            }
        }
    };

    public abstract String generate();
}
