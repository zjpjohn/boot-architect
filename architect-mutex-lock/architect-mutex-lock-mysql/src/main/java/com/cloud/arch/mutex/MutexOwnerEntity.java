package com.cloud.arch.mutex;


import com.cloud.arch.mutex.core.MutexOwner;

public class MutexOwnerEntity extends MutexOwner {

    private final String  mutex;
    private       Integer version;
    private       long    currentAt;

    public MutexOwnerEntity(String mutex, String ownerId, long acquireAt, long ttlAt, long transitionAt) {
        super(ownerId, acquireAt, ttlAt, transitionAt);
        this.mutex = mutex;
    }

    public String getMutex() {
        return mutex;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public long getCurrentAt() {
        return currentAt;
    }

    public void setCurrentAt(long currentAt) {
        this.currentAt = currentAt;
    }
}
