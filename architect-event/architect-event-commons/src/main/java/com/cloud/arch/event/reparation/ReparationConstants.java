package com.cloud.arch.event.reparation;

public class ReparationConstants {

    private ReparationConstants() {
        throw new UnsupportedOperationException("this is static factory class,not support construct.");
    }

    /**
     * 补偿事件请求地址
     */
    public static String REPARATION_PATH          = "/";
    /**
     * 执行器请求补偿服务器accessToken请求头
     */
    public static String REPARATION_ACCESS_HEADER = "Event-Access-Token";
    /**
     * 执行器请求补偿服务器appKey请求头
     */
    public static String REPARATION_KEY_HEADER    = "Event-Access-Key";

}
