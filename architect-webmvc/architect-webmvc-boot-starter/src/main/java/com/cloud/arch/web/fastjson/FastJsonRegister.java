package com.cloud.arch.web.fastjson;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import com.cloud.arch.enums.Value;
import lombok.extern.slf4j.Slf4j;
import org.atteo.classindex.ClassIndex;
import org.springframework.beans.factory.InitializingBean;

import java.util.stream.StreamSupport;

@Slf4j
public class FastJsonRegister implements InitializingBean {

    /**
     * 枚举序列化专用 provider。
     * 只注册到本字段，不写入 JSONFactory 的全局默认 provider，
     * 避免 JSONB（Dubbo 的 fastjson2 序列化）等全局使用方被动继承枚举的 {label,value} 写法。
     * 继承全局 provider 的 creator，保持 writer 生成方式一致。
     */
    private static final ObjectWriterProvider WRITER_PROVIDER =
        new ObjectWriterProvider(JSONFactory.getDefaultObjectWriterProvider().getCreator());

    /**
     * 加密响应体序列化使用的 provider
     */
    public static ObjectWriterProvider writerProvider() {
        return WRITER_PROVIDER;
    }

    private void registerValueEnums() {
        Iterable<Class<? extends Value>> classes = ClassIndex.getSubclasses(Value.class);
        StreamSupport.stream(classes.spliterator(), false)
                     .filter(clazz -> !clazz.isInterface() && clazz.isEnum())
                     .forEach(this::registerEnum);
    }

    private <K extends Comparable<K>, T extends Value<K>> void registerEnum(Class<T> type) {
        WRITER_PROVIDER.registerIfAbsent(type, new EnumValueWriter());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.registerValueEnums();
    }

}
