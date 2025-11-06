package com.cloud.arch.hotkey.core.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyHotModel {

    /**
     * 缓存key
     */
    private String  key;
    /**
     * 缓存规则名称
     */
    private String  cache;
    /**
     * 是否是热key
     */
    private boolean hot;

}
