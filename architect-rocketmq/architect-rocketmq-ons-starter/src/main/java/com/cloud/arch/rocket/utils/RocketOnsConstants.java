package com.cloud.arch.rocket.utils;

public class RocketOnsConstants {

    public static final Integer DEFAULT_INTERVAL                     = 7;
    public static final String  DEFAULT_CLEAN_CRON                   = "0 0 0 1/7 * ?";
    public static final String  CHECK_SERVICE_BEAN_NAME              = "ons-check-executor";
    public static final String  ONS_RECOGNISE_BEAN_NAME              = "ons-recognise-handler";
    public static final String  LOCAL_TRANSACTION_CHECKER_BEAN_NAME  = "ons-transaction-checker";
    public static final String  TRANSACTION_SENDER_ADVISOR_BEAN_NAME = "ons-transaction-advisor";
    public static final String  ONS_ALL_TAG_REGEX                    = "*";
    public static final String  ONS_COMPOSITE_TAG_DELIMITER          = "||";

}
