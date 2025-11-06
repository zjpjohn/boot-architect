package com.cloud.arch.event.codec;

public interface EventCodec {

    /**
     * 事件序列化
     *
     * @param event 事件内容
     */
    String encode(Object event);

    /**
     * 事件反序列化
     *
     * @param data 事件内容
     * @param type 事件类型
     */
    Object decode(String data, Class<?> type);

}
