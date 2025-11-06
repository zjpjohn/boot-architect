package com.cloud.arch.web.dict;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryValue {
    /**
     * 字典项名称
     */
    private String label;
    /**
     * 字典项数据
     */
    private Object value;

}
