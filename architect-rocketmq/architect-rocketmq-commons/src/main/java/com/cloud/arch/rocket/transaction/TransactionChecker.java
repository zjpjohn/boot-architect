package com.cloud.arch.rocket.transaction;

public interface TransactionChecker {

    /**
     * 事务状态回查
     *
     * @param topic 消息topic
     * @param tag   消息tag
     * @param key   消息key
     */
    TransactionState checkTransaction(String topic, String tag, String key);

}
