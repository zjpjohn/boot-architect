package com.cloud.arch.hotkey.core.key;

import java.util.List;

public interface IKeyCollector<T, V> {

    /**
     * 锁定后返回数据
     */
    List<V> lockAndGet();

    /**
     * 收集输入的参数
     */
    void collect(T t);

    void finishOnce();
}
