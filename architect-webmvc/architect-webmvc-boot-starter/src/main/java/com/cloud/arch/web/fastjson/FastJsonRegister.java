package com.cloud.arch.web.fastjson;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.enums.Value;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.factory.InitializingBean;

import java.util.stream.StreamSupport;

public class FastJsonRegister implements InitializingBean {

    private void registerValueEnums() {
        Iterable<Class<? extends Value>> classes = ClassIndex.getSubclasses(Value.class);
        StreamSupport.stream(classes.spliterator(), false)
                     .filter(clazz -> !clazz.isInterface() && clazz.isEnum())
                     .forEach(this::registerEnum);
    }

    private <K extends Comparable<K>, T extends Value<K>> void registerEnum(Class<T> type) {
        JSON.registerIfAbsent(type, new EnumValueWriter());
        JSON.registerIfAbsent(type, new EnumValueReader<>(type), true);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.registerValueEnums();
    }

}
