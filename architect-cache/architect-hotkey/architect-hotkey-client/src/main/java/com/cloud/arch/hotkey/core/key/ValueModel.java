package com.cloud.arch.hotkey.core.key;

import com.cloud.arch.hotkey.utils.HotkeyConstants;
import lombok.Data;


@Data
public class ValueModel {

    //对象创建时间
    private Long    createTime;
    private Integer duration;
    private Object  value;
    private boolean filled;

    /**
     * 初次设置进行热key填充
     * 缓存值并未填充
     */
    public static ValueModel mark(Integer duration) {
        ValueModel valueModel = new ValueModel();
        valueModel.setCreateTime(System.currentTimeMillis());
        valueModel.setFilled(false);
        valueModel.setDuration(duration);
        valueModel.setValue(HotkeyConstants.MAGIC_NUMBER);
        return valueModel;
    }

    /**
     * 基于缓存值构建
     *
     * @param value 缓存值
     */
    public static ValueModel value(Object value, Integer duration) {
        ValueModel valueModel = new ValueModel();
        valueModel.setCreateTime(System.currentTimeMillis());
        valueModel.setFilled(true);
        valueModel.setDuration(duration);
        valueModel.setValue((value != null ? value : HotkeyConstants.MAGIC_NUMBER));
        return valueModel;
    }

}
