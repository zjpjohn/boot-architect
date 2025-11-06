package com.cloud.arch.event.codec;


import com.alibaba.fastjson2.JSON;

public class FastJson2EventCodec implements EventCodec {

    /**
     * 事件序列化
     *
     * @param event 事件内容
     */
    @Override
    public String encode(Object event) {
        return JSON.toJSONString(event);
    }

    /**
     * 事件反序列化
     *
     * @param data 事件内容
     * @param type 事件类型
     */
    @Override
    public Object decode(String data, Class<?> type) {
        return JSON.parseObject(data, type);
    }

}
