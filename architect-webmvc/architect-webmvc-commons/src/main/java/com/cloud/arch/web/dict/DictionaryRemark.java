package com.cloud.arch.web.dict;

import com.cloud.arch.enums.Value;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@SuppressWarnings({"rawtypes"})
public class DictionaryRemark implements Comparable<DictionaryRemark> {

    //字典数据名称
    private String              name;
    //字典数据类型
    private String              type;
    //字典描述说明
    private String              remark;
    //字典值集合
    private Map<String, Object> values;

    public DictionaryRemark(Dictionary dictionary, Class<? extends Value> type) {
        this.name = dictionary.name();
        this.type = dictType(dictionary, type);
        this.remark = dictionary.remark();
        this.values = this.typeToValue(type);
    }

    private String dictType(Dictionary dictionary, Class<? extends Value> target) {
        Type[] interfaces = target.getGenericInterfaces();
        for (Type type : interfaces) {
            if (type instanceof ParameterizedType parameterizedType) {
                Type   rawType = parameterizedType.getRawType();
                Type[] types   = parameterizedType.getActualTypeArguments();
                if (rawType.equals(Value.class) && types.length > 0 && (types[0] instanceof Class<?> clazz)) {
                    return clazz.getSimpleName().toLowerCase();
                }
            }
        }
        return dictionary.type();
    }

    private Map<String, Object> typeToValue(Class<? extends Value> type) {
        Map<String, Object> result = Maps.newLinkedHashMap();
        Arrays.asList(type.getEnumConstants()).forEach(e -> result.put(e.label(), e.value()));
        return result;
    }

    public List<DictionaryValue> toValueList() {
        return this.values.entrySet().stream().map(e -> new DictionaryValue(e.getKey(), e.getValue())).toList();
    }

    @Override
    public int compareTo(DictionaryRemark o) {
        return this.name.compareTo(o.name);
    }
}
