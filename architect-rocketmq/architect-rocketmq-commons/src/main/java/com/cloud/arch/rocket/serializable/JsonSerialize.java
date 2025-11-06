package com.cloud.arch.rocket.serializable;

import com.alibaba.fastjson2.JSON;

public class JsonSerialize implements Serialize {

    @Override
    public <T> byte[] serialize(T target) {
        try {
            return JSON.toJSONBytes(target);
        } catch (Exception error) {
            throw new RuntimeException("json序列化异常", error);
        }
    }

    @Override
    public <T> T deSerialize(byte[] data, Class<T> clazz) {
        try {
            return JSON.parseObject(data, clazz);
        } catch (Exception error) {
            throw new RuntimeException("json反序列化异常", error);
        }
    }

}
