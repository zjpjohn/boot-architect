package com.cloud.arch.hotkey.core.key;


import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.KeyCountModel;

import java.util.List;

public interface IKeyPusher {

    /**
     * 发送待测热key
     *
     * @param appName 应用名称
     * @param models  待测热key数据
     */
    void send(String appName, List<HotKeyModel> models);

    /**
     * 发送热key统计数据
     *
     * @param appName 应用名称
     * @param models  热key统计数据
     */
    void sendCount(String appName, List<KeyCountModel> models);
}
