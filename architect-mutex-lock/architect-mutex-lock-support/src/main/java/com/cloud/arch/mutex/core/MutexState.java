package com.cloud.arch.mutex.core;

import lombok.Getter;

@Getter
public class MutexState {

    public static final MutexState NONE = new MutexState(MutexOwner.NONE, MutexOwner.NONE);

    private final MutexOwner before;
    private final MutexOwner after;

    public MutexState(MutexOwner before, MutexOwner after) {
        this.before = before;
        this.after  = after;
    }

    public boolean isChanged() {
        return !before.isOwner(after.getOwnerId());
    }

    public boolean isOwner(String contenderId) {
        return after.isOwner(contenderId);
    }

    public boolean isAcquired(String contenderId) {
        return isChanged() && isOwner(contenderId);
    }

    public boolean isReleased(String contenderId) {
        return isChanged() && before.isOwner(contenderId);
    }

    public boolean isInTtl(String contenderId) {
        return isOwner(contenderId) && after.isInTtl(contenderId);
    }

}
