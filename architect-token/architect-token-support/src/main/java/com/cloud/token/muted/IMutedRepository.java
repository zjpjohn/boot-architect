package com.cloud.token.muted;

public interface IMutedRepository {

    /**
     * 保存封禁信息
     */
    void save(String key, int level, long timeout);

    /**
     * 判断是否封禁
     */
    boolean isMuted(String key, int level);

    /**
     * 查询封禁等级
     */
    int getMuted(String key);

    /**
     * 解除封禁
     */
    boolean cancel(String key);

}
