package com.cloud.arch.page;

import com.cloud.arch.enums.Value;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Optional;

@Getter
public enum PageEnums {
    LIMIT("limit") {
        @Override
        <T extends PageQuery> void valFunction(PageCondition condition, Field field, T target) throws Exception {
            Integer limit = Optional.ofNullable(field.get(target)).map(v -> (Integer)v).orElse(10);
            condition.setLimit(limit);
        }
    },
    PAGE("page") {
        @Override
        <T extends PageQuery> void valFunction(PageCondition condition, Field field, T target) throws Exception {
            Integer page = Optional.ofNullable(field.get(target)).map(v -> (Integer)v).orElse(1);
            condition.setPage(page);
        }
    },
    SORT("sort") {
        @Override
        <T extends PageQuery> void valFunction(PageCondition condition, Field field, T target) throws Exception {
            Integer sort =
                Optional.ofNullable(field.get(target)).map(v -> (Integer)v).orElse(SortEnums.LATEST_CREATE.getSort());
            condition.setParam(this.getName(), sort);
        }
    },
    PARAM("param") {
        @Override
        <T extends PageQuery> void valFunction(PageCondition condition, Field field, T target) throws Exception {
            Object value = field.get(target);
            if (value == null || ((value instanceof String) && StringUtils.isBlank((String)value))) {
                return;
            }
            Class<?> type = field.getType();
            if (Enum.class.isAssignableFrom(type) && Value.class.isAssignableFrom(type)) {
                value = ((Value)value).value();
            }
            String key = Optional.ofNullable(field.getAnnotation(Alias.class))
                                 .map(Alias::value)
                                 .filter(StringUtils::isNotBlank)
                                 .orElse(field.getName());
            condition.setParam(key, value);
        }
    };

    private final String name;

    PageEnums(String name) {
        this.name = name;
    }

    abstract <T extends PageQuery> void valFunction(PageCondition condition, Field field, T target) throws Exception;

    public <T extends PageQuery> void setValue(PageCondition condition, Field field, T target) {
        try {
            valFunction(condition, field, target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
