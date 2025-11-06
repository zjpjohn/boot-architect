package com.cloud.arch.rocket.utils;

public class RocketmqConstants {

    public static final String  PREFIX                         = "rocketmq_";
    public static final String  KEYS                           = "KEYS";
    public static final String  TAGS                           = "TAGS";
    public static final String  TOPIC                          = "TOPIC";
    public static final String  MESSAGE_ID                     = "MESSAGE_ID";
    public static final String  BORN_TIMESTAMP                 = "BORN_TIMESTAMP";
    public static final String  WAIT_STORE_MSG_OK              = "WAIT_STORE_MSG_OK";
    public static final String  BORN_HOST                      = "BORN_HOST";
    public static final String  FLAG                           = "FLAG";
    public static final String  QUEUE_ID                       = "QUEUE_ID";
    public static final String  SYS_FLAG                       = "SYS_FLAG";
    public static final String  TRANSACTION_ID                 = "TRANSACTION_ID";
    public static final String  ATTR_VALUE                     = "value";
    public static final String  ATTR_BASE_PACKAGES             = "basePackages";
    public static final String  ATTR_BASE_PACKAGE_CLASSES      = "basePackageClasses";
    public static final int     BATCH_SIZE                     = 20;
    public static final Integer DEFAULT_INTERVAL               = 7;
    public static final String  DEFAULT_CLEAN_CRON             = "0 0 0 1/7 * ?";
    public static final String  JDBC_TRANSACTION_CHECK         = "rocket_jdbc_transaction_check_service";
    public static final String  TRANSACTION_SENDER_INTERCEPTOR = "rocket_transaction_sender_interceptor";
    public static final String  ROCKET_TRANSACTION_ADVISOR     = "rocket_transaction_advisor";
    public static final String  TRANSACTION_GARBAGE_JOB        = "rocket_transaction_garbage_job";
    public static final String  IDEMPOTENT_GARBAGE_JOB         = "rocket_idempotent_garbage_job";
    public static final String  ROCKET_CONSUMER_PROCESSOR      = "rocket_consumer_processor_bean";
    public static final String  CHECK_SERVICE_BEAN_NAME        = "rocket-check-executor";
    public static final String  TRANSACTION_EXECUTOR_KEY       = "rocket_tx_executor";
    public static final String  SENDER_RECOGNISE_BEAN_NAME     = "sender-recognise-handler";
    public static final String  ROCKET_ALL_TAG_REGEX           = "*";
    public static final String  ROCKET_TAG_DELIMITER           = "||";

}
