package com.cloud.arch.rocket.serializable;

public interface Serialize {

    <T> byte[] serialize(T target);

    <T> T deSerialize(byte[] data, Class<T> clazz);
}
