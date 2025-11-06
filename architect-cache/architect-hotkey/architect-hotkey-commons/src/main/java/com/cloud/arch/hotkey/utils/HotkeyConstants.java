package com.cloud.arch.hotkey.utils;

public class HotkeyConstants {

    public static String PING                 = "ping";
    public static String PONG                 = "pong";
    /**
     * 魔法数值占位符
     */
    public static int    MAGIC_NUMBER         = 0x12fcf76;
    /**
     * netty分隔符
     */
    public static String DELIMITER            = "$(* *)$";
    /**
     * 单次发送包最大4MB
     */
    public static int    MAX_LENGTH           = 4 * 1024 * 1024;
    /**
     * 数量统计时，rule+时间 组成key用的分隔符
     */
    public static String COUNT_DELIMITER      = "#**#";
    /**
     * 手动删除自动探测的key，存储标识
     */
    public static String DEFAULT_DELETE_VALUE = "#[DELETE]#";
    /**
     * 手动创建热key存储标识
     */
    public static String DEFAULT_CREATE_VALUE = "#[CREATE]#";

}
