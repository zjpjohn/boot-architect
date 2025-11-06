package com.cloud.arch.web.dict;

import com.cloud.arch.enums.Value;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DictionaryFactory implements InitializingBean {

    private final Map<String, DictionaryRemark> dictionaryCache = Maps.newHashMap();

    @Override
    public void afterPropertiesSet() throws Exception {
        this.scanDictionary();
    }

    /**
     * 扫描枚举字典数据
     */
    private void scanDictionary() {
        ClassIndex.getAnnotated(Dictionary.class).forEach(this::extractFrom);
    }

    /**
     * 提取单个字典数据
     */
    private void extractFrom(Class<?> clazz) {
        if (!Value.class.isAssignableFrom(clazz) || !Enum.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("@Dictionary annotation use illegal.");
        }
        Class<? extends Value> type       = (Class<? extends Value>) clazz;
        Dictionary             annotation = clazz.getAnnotation(Dictionary.class);
        String                 name       = annotation.name();
        if (StringUtils.isBlank(name) || dictionaryCache.containsKey(name)) {
            throw new IllegalArgumentException("dictionary name must not be null or has duplicated name.");
        }
        dictionaryCache.put(name, new DictionaryRemark(annotation, type));
    }

    /**
     * 查询系统字典描述集合
     */
    public List<DictionaryRemark> list() {
        return dictionaryCache.values().stream().toList();
    }

    /**
     * 根据字典名称查询字典数据集合
     */
    public List<DictionaryValue> of(String name) {
        return Optional.ofNullable(dictionaryCache.get(name))
                       .map(DictionaryRemark::toValueList)
                       .orElseGet(Collections::emptyList);
    }

}
