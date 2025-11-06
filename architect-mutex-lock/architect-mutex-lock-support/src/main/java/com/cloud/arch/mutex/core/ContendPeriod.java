package com.cloud.arch.mutex.core;

import java.util.concurrent.ThreadLocalRandom;

public class ContendPeriod {

    private final String contenderId;

    public ContendPeriod(String contenderId) {
        this.contenderId = contenderId;
    }

    public long ensureNextDelay(MutexOwner mutexOwner) {
        long nextDelay = nextDelay(mutexOwner);
        return nextDelay < 0 ? 0 : nextDelay;
    }

    public long nextDelay(MutexOwner mutexOwner) {
        if (mutexOwner.isOwner(contenderId)) {
            return nextOwnerDelay(mutexOwner);
        }
        return nextContenderDelay(mutexOwner);
    }

    public static long nextOwnerDelay(MutexOwner mutexOwner) {
        return mutexOwner.getTtlAt() - System.currentTimeMillis();
    }

    public static long nextContenderDelay(MutexOwner mutexOwner) {
        final long transition = mutexOwner.getTransitionAt() - mutexOwner.getTtlAt();
        final long max        = 1000;
        long       min        = -200;
        if (transition == 0) {
            min = 0;
        }
        final long random = ThreadLocalRandom.current().nextLong(min, max);
        final long now    = System.currentTimeMillis();
        return mutexOwner.getTransitionAt() - now + random;
    }


}
