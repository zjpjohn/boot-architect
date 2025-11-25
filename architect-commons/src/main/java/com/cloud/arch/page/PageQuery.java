package com.cloud.arch.page;

import com.cloud.arch.enums.Value;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
public class PageQuery implements Serializable {
    
    public static final String PAGE_KEY  = "page";
    public static final String LIMIT_KEY = "limit";

    @Min(value = 1, message = "page must greater than 1")
    private Integer page  = 1;
    @Range(min = 1, max = 1000, message = "page size range in 1-1000")
    private Integer limit = 10;

    public PageCondition from() {
        final PageCondition condition = new PageCondition();
        condition.setPage(page);
        condition.setLimit(limit);
        List<Field> fields = this.getFields();
        fields.stream().filter(this::checkField).forEach(field -> {
            field.setAccessible(true);
            Object value = this.value(field);
            if (value != null) {
                String key = this.key(field);
                condition.setParam(key, value);
            }
        });
        return condition;
    }

    private List<Field> getFields() {
        Class<?>    clazz  = this.getClass();
        List<Field> fields = Lists.newArrayList();
        while (clazz != null && !Object.class.equals(clazz)) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private boolean checkField(Field field) {
        String name      = field.getName();
        int    modifiers = field.getModifiers();
        return Modifier.isPrivate(modifiers)
                && !Modifier.isStatic(modifiers)
                && !Modifier.isFinal(modifiers)
                && Objects.isNull(field.getAnnotation(Ignore.class))
                && !name.equals(PAGE_KEY)
                && !name.equals(LIMIT_KEY);
    }

    private String key(Field field) {
        Alias alias = field.getAnnotation(Alias.class);
        return Optional.ofNullable(alias).map(Alias::value).filter(StringUtils::isNotBlank).orElse(field.getName());
    }

    private Object value(Field field) {
        try {
            Object value = field.get(this);
            if (value == null || ((value instanceof String str) && StringUtils.isBlank(str))) {
                return null;
            }
            Class<?> type = field.getType();
            if (Enum.class.isAssignableFrom(type) && Value.class.isAssignableFrom(type)) {
                value = ((Value) value).value();
            }
            return value;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
