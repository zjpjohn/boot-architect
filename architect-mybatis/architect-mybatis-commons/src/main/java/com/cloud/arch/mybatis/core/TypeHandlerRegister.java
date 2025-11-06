package com.cloud.arch.mybatis.core;

import com.cloud.arch.mybatis.annotations.TypeHandler;
import lombok.experimental.UtilityClass;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.atteo.classindex.ClassIndex;

import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class TypeHandlerRegister {

    private static final Class<TypeHandler> TYPE_HANDLER_CLASS = TypeHandler.class;

    /**
     * 注册枚举和JSON类型转换器
     */
    public static void registry(TypeHandlerRegistry registry) {
        Iterable<Class<?>> classes = ClassIndex.getAnnotated(TYPE_HANDLER_CLASS);
        classes.forEach(v -> {
            TypeHandler annotation = v.getAnnotation(TYPE_HANDLER_CLASS);
            annotation.type().register(v, registry);
        });
        Type.JSON.register(List.class, registry);
        Type.JSON.register(Map.class, registry);
        Type.JSON.register(Set.class, registry);
    }

}
