package com.cloud.arch.mutex.core;

import lombok.Getter;

@Getter
public class MutexOwner {

    public static final String     NONE_OWNER_ID = "";
    public static final MutexOwner NONE          = new MutexOwner(NONE_OWNER_ID, 0, 0, 0);

    /**
     * 资源持有者标识
     */
    private final String ownerId;
    /**
     * 获取竞争资源的时间戳
     */
    private final long   acquireAt;
    /**
     * 持有竞争资源到期时间
     */
    private final long   ttlAt;
    /**
     * 持有资源缓冲器/过渡期
     * 1.为了资源持有权稳定，当前领导者在过渡期内优先续期
     * 2.用户缓冲领导者任务执行时间
     */
    private final long   transitionAt;

    public MutexOwner(String ownerId) {
        this(ownerId, System.currentTimeMillis(), Long.MAX_VALUE, Long.MAX_VALUE);
    }

    public MutexOwner(String ownerId, long acquireAt, long ttlAt, long transitionAt) {
        this.ownerId      = ownerId;
        this.acquireAt    = acquireAt;
        this.ttlAt        = ttlAt;
        this.transitionAt = transitionAt;
    }

    public boolean isOwner(String contenderId) {
        return this.ownerId.equals(contenderId);
    }

    public long getCurrentAt() {
        return System.currentTimeMillis();
    }

    public boolean isInTtl() {
        return ttlAt > this.getCurrentAt();
    }

    public boolean isInTtl(String contenderId) {
        return isOwner(contenderId) && isInTtl();
    }

    public boolean isInTransition() {
        return this.transitionAt >= getTransitionAt();
    }

    public boolean isInTransition(String contenderId) {
        return isOwner(contenderId) && this.isInTransition();
    }

    public boolean hasOwner() {
        return this.transitionAt >= getTransitionAt();
    }

}
