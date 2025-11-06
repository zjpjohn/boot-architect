package com.cloud.arch.mutex;


public interface IMutexOwnerRepository {

    boolean initMutex(String mutex);

    MutexOwnerEntity getOwner(String mutex);

    boolean acquire(String mutex, String contenderId, long ttl, long transition);

    MutexOwnerEntity acquireAndGetOwner(String mutex, String contenderId, long ttl, long transition);

    boolean release(String mutex, String contenderId);

}
