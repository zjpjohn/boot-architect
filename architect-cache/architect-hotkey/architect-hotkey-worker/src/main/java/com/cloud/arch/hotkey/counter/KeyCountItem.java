package com.cloud.arch.hotkey.counter;

import com.cloud.arch.hotkey.model.KeyCountModel;
import lombok.Data;

import java.util.List;

@Data
public class KeyCountItem {

    private final String              appName;
    private final Long                createTime;
    private final List<KeyCountModel> list;

    public KeyCountItem(String appName, Long createTime, List<KeyCountModel> list) {
        this.appName    = appName;
        this.createTime = createTime;
        this.list       = list;
    }

    public String getAppName() {
        return appName;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public List<KeyCountModel> getList() {
        return list;
    }
}
