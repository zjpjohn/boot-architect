package com.cloud.arch.rocket.meta.processor.impl;

import com.cloud.arch.rocket.meta.SenderMetadata;
import com.cloud.arch.rocket.meta.processor.MethodParameterProcessor;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

public class CallbackParameterProcessor implements MethodParameterProcessor {

    /**
     * 注解参数处理器构建元数据
     *
     * @param metadata 元数据
     * @param type     参数类型
     * @param index    参数位置
     */
    @Override
    public void buildMeta(SenderMetadata metadata, Class<?> type, int index) {
        Method method = metadata.getMethod();
        Assert.isNull(metadata.getAsync(),
                      String.format("方法%s.%s中有多个SendCallBack参数",
                                    method.getDeclaringClass().getSimpleName(),
                                    method.getName()));
        metadata.setAsync(index);
    }
}
