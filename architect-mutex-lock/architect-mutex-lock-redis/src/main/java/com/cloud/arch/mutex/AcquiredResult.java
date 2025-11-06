package com.cloud.arch.mutex;

public class AcquiredResult {

    private final long   current = System.currentTimeMillis();
    private       String ownerId;
    private       long   ttl;

    public AcquiredResult() {
    }

    public AcquiredResult(String ownerId, long ttl) {
        this.ownerId = ownerId;
        this.ttl     = ttl;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getTransitionAt() {
        return current + ttl;
    }

}
