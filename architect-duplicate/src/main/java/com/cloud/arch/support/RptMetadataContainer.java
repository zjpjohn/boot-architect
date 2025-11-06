package com.cloud.arch.support;

import com.cloud.arch.annotation.RptField;
import com.cloud.arch.utils.CollectionUtils;
import com.google.common.collect.Maps;

import java.util.*;

public class RptMetadataContainer {

    /**
     * 缓存重复校验元数据
     */
    private static final Map<Class<?>, List<RptMetadata>> metaContainer = Maps.newConcurrentMap();

    /**
     * 计算指定对象校验重复字段信息
     */
    public static List<RptFieldValue> compute(Object target) {
        List<RptMetadata> metaList = getMetaList(target);
        if (CollectionUtils.isEmpty(metaList)) {
            return Collections.emptyList();
        }
        return metaList.stream().map(meta -> meta.rptValue(target)).filter(Objects::nonNull).toList();
    }

    /**
     * 获取重复字段元数据信息
     */
    public static List<RptMetadata> getMetaList(Object target) {
        return metaContainer.computeIfAbsent(target.getClass(), RptMetadataContainer::metaList);
    }

    /**
     * 获取对象全部校验重复字段元数据
     */
    private static List<RptMetadata> metaList(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                     .filter(field -> field.getAnnotation(RptField.class) != null)
                     .map(field -> new RptMetadata(field.getAnnotation(RptField.class), field))
                     .toList();
    }

}
