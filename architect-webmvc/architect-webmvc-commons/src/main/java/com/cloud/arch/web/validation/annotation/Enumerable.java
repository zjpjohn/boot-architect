package com.cloud.arch.web.validation.annotation;


import com.cloud.arch.enums.Value;
import com.cloud.arch.web.validation.validator.EnumerableValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = EnumerableValidator.class)
public @interface Enumerable {

    String message() default "invalid argument condition";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 可枚举数据范围
     */
    String[] ranges() default {};

    /**
     * 枚举类范围限制
     */
    Class<? extends Value<? extends Comparable<?>>>[] enums() default {};

}
