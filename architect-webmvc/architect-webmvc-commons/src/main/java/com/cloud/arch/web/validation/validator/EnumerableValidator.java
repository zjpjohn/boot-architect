package com.cloud.arch.web.validation.validator;

import com.cloud.arch.enums.Value;
import com.cloud.arch.enums.ValueType;
import com.cloud.arch.web.validation.annotation.Enumerable;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;

@Slf4j
public class EnumerableValidator implements ConstraintValidator<Enumerable, Comparable<?>> {

    /**
     * 枚举数据值范围
     */
    private String[]                         ranges;
    /**
     * 枚举值范围
     */
    private Value<? extends Comparable<?>>[] enums;

    @Override
    public void initialize(Enumerable constraint) {
        this.ranges = constraint.ranges();
        //枚举值数据优先，即：配置了可枚举数据值，枚举类校验不起作用
        if (this.ranges != null && this.ranges.length > 0) {
            return;
        }
        Class<? extends Value<?>>[] enumsClass = constraint.enums();
        if (enumsClass == null || enumsClass.length == 0) {
            throw new IllegalArgumentException("ranges or enumerable class must be not be both null.");
        }
        if (!Enum.class.isAssignableFrom(enumsClass[0])) {
            throw new IllegalArgumentException("enumerable class must be enum class.");
        }
        this.enums = enumsClass[0].getEnumConstants();
        String typeName = this.enums[0].value().getClass().getName();
        if (ValueType.of(typeName) == null) {
            throw new IllegalArgumentException(
                    "enum value type only support [ 'String' , 'Integer' , 'Long' , 'Double' , 'Float' , 'Short' ] , but this value type is '"
                            + typeName
                            + "'");
        }
    }

    @Override
    public boolean isValid(Comparable<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (this.ranges != null && this.ranges.length > 0) {
            return isRanges(value);
        }
        if (this.enums != null && this.enums.length > 0) {
            return isEnumerable(value);
        }
        return true;
    }

    /**
     * 可枚举的值范围
     *
     * @param value 待校验的数据
     */
    private boolean isRanges(Comparable<?> value) {
        if ((value instanceof String && StringUtils.isBlank((String) value))) {
            return true;
        }
        final Comparable<?> valued = adaptValue(value);
        ValueType           type   = ValueType.of(valued.getClass().getName());
        return Arrays.stream(ranges).anyMatch(source -> type.compareTo(valued, source) == 0);
    }

    /**
     * 枚举类型限定范围校验
     *
     * @param value 待校验的数据
     */
    private boolean isEnumerable(Comparable<?> value) {
        if ((value instanceof String && StringUtils.isBlank((String) value))) {
            return Boolean.TRUE;
        }
        final Comparable<?> valued = adaptValue(value);
        return Arrays.stream(enums).anyMatch(v -> Objects.equals(v.value(), valued));
    }

    /**
     * 适配校验参数值为Value枚举值
     */
    private Comparable<?> adaptValue(Comparable<?> value) {
        if (value instanceof Enum && value instanceof Value) {
            return ((Value<?>) value).value();
        }
        return value;
    }

}
