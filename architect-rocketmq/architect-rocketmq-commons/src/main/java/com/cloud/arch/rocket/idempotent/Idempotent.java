package com.cloud.arch.rocket.idempotent;

/**
 * 消费幂等类型
 * 后续考虑增加自定义模式
 */
public enum Idempotent {

    JDBC("jdbc_idempotent_check"),
    TRANSACTION("jdbc_transaction_idempotent_check"),
    NONE("");

    private final String name;

    Idempotent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static final String JDBC_IDEMPOTENT_CHECK             = "jdbc_idempotent_check";
    public static final String JDBC_TRANSACTION_IDEMPOTENT_CHECK = "jdbc_transaction_idempotent_check";

}
