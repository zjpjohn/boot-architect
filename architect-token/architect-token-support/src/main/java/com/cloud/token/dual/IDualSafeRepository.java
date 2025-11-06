package com.cloud.token.dual;

public interface IDualSafeRepository {

    void save(String key, long timeout);

    boolean isSafe(String key);

}
