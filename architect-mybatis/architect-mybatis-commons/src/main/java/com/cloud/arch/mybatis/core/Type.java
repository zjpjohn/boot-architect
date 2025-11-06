package com.cloud.arch.mybatis.core;

import com.cloud.arch.enums.Value;
import com.cloud.arch.mybatis.core.handler.EnumTypeHandler;
import com.cloud.arch.mybatis.core.handler.JsonTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;

@Slf4j
@SuppressWarnings("unchecked")
public enum Type {

    JSON {
        @Override
        public void register(Class<?> clazz, TypeHandlerRegistry registry) {
            registry.register(clazz, JdbcType.VARCHAR, new JsonTypeHandler(clazz));
        }
    },
    ENUM {
        @Override
        public void register(Class<?> clazz, TypeHandlerRegistry registry) {
            if (Value.class.isAssignableFrom(clazz) && Enum.class.isAssignableFrom(clazz)) {
                EnumTypeHandler handler = new EnumTypeHandler(clazz);
                registry.register(clazz, handler.getValueType().jdbcType(), handler);
            }
        }
    };

    public abstract void register(Class<?> clazz, TypeHandlerRegistry registry);
}
