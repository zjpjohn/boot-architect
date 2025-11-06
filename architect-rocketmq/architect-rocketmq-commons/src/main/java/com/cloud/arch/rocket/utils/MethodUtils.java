package com.cloud.arch.rocket.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodUtils {

    /**
     * 判断方法是否为 default
     *
     * @param method 方法信息
     */
    public static boolean isDefault(Method method) {
        final int SYNTHETIC = 0x00001000;
        return ((method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC | SYNTHETIC))
                == Modifier.PUBLIC) && method.getDeclaringClass().isInterface();
    }

}
