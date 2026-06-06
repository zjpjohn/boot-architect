package com.cloud.arch.web.mask;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {

    MaskType type() default MaskType.CUSTOM;

    double ratio() default 0.5;

    char masker() default '*';

    int minLength() default 3;

}
