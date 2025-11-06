package com.cloud.arch.event.rocksdb;

public class RocksDbConstants {

    private RocksDbConstants() {
        throw new UnsupportedOperationException("this class is static constants class, not support this construction.");
    }

    public static String DEFAULT_FAMILY = "default";
    public static String EVENT_TIME     = "time:id";
    public static String EVENT_MESSAGE  = "event:id";

}
