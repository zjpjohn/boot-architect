package com.cloud.arch.rocket.transaction;

import java.util.Date;
import java.util.function.Consumer;

public interface TransactionChecker {

    /**
     * 开始本地事务
     */
    void begin();

    /**
     * 提交本地事务
     */
    void commit();

    /**
     * 回滚本地事务
     */
    void rollback();

    /**
     * 标记本地事务
     *
     * @param key   事务标识
     * @param state 事务状态
     */
    void mark(String key, TransactionState state);

    /**
     * 检查本地事务
     *
     * @param key 事务标识
     */
    TransactionState checkTransaction(String key);

    /**
     * 回收本地事务状态信息
     * 清理历史数据
     *
     * @param date 指定值日期
     */
    void garbageState(Date date);

    /**
     * 执行本地事务业务
     *
     * @param args     方法参数
     * @param consumer lambda
     */
    void handle(Object[] args, Consumer<Object[]> consumer);

}
