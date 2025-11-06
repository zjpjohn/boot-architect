package com.cloud.arch.hotkey.core.key;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@SuppressWarnings("unchecked")
public class DetectedValue {

    /**
     * 热key探测后缓存的本地值
     */
    private Object  value;
    /**
     * 是否是热key
     */
    private boolean hot;
    /**
     * 热key标识
     */
    private String  key;
    /**
     * 是否获取成功
     */
    private boolean success;
    /**
     * 本地是否已缓存
     */
    private boolean cached;

    public DetectedValue(String key, boolean success) {
        this.key     = key;
        this.success = success;
    }

    public <T> T toValue() {
        return (T) value;
    }

}
