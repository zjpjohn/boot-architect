package com.cloud.arch.hotkey.utils;


import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.model.HotKeyModel;

public class HotKeyPathTool {
    /**
     * app的热key存放地址，client会监听该地址，当有热key变化时会响应
     */
    public static String keyPath(HotKeyModel hotKeyModel) {
        return ConfigConstant.hotKeyPath + hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
    }

    /**
     * worker将热key推送到该地址，供dashboard监听入库做记录
     */
    public static String keyRecordPath(HotKeyModel hotKeyModel) {
        return ConfigConstant.hotKeyRecordPath + hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
    }
}
