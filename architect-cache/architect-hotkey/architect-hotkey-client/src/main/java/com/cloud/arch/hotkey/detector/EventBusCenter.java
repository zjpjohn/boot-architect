package com.cloud.arch.hotkey.detector;

import com.google.common.eventbus.EventBus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventBusCenter {

    private static final EventBus EVENT_BUS = new EventBus();

    /**
     * 注册事件订阅期
     */
    public static void register(Object subscriber) {
        EVENT_BUS.register(subscriber);
    }

    /**
     * 取消事件订阅期
     */
    public static void unregister(Object subscriber) {
        EVENT_BUS.unregister(subscriber);
    }

    /**
     * 发布事件
     */
    public static void post(Object event) {
        EVENT_BUS.post(event);
    }
}
