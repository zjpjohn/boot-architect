package com.cloud.arch.transaction.core;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

@UtilityClass
public class AsyncTxSharding {

    /**
     * 空shardingKey
     */
    public static final  String              EMPTY_SHARDING_KEY = "";
    /**
     * 当前线程异步任务分库分表shardingKey
     */
    private static final ThreadLocal<String> shardingContext    = ThreadLocal.withInitial(() -> EMPTY_SHARDING_KEY);

    /**
     * 设置当前异步任务集合的shardingKey
     *
     * @param shardingKey 分库分表key
     */
    public static void shardingKey(String shardingKey) {
        Assert.state(StringUtils.isNotBlank(shardingKey), "sharding key must not be null.");
        shardingContext.set(shardingKey);
    }

    /**
     * 获取当前线程异步任务的shardingKey
     */
    public static String shardingKey() {
        return shardingContext.get();
    }

    /**
     * 清空当前线程分片键上下文
     */
    public static void clear() {
        shardingContext.remove();
    }

}
