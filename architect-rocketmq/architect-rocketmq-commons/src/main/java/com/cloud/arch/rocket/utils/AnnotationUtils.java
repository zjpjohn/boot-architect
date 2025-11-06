package com.cloud.arch.rocket.utils;

import java.lang.annotation.Annotation;

public class AnnotationUtils {

    /**
     * 查找注解对应的参数位置
     *
     * @param annotations    参数注解集合
     * @param annotationType 指定注解类型
     */
    public static Integer annotationIndex(Annotation[][] annotations, Class<? extends Annotation> annotationType) {
        for (int i = 0; i < annotations.length; i++) {
            Annotation[] annArray = annotations[i];
            for (Annotation ann : annArray) {
                if (annotationType.equals(ann.annotationType())) {
                    return i;
                }
            }
        }
        return null;
    }

}
