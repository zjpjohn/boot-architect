package com.cloud.arch.mybatis.core;

import com.cloud.arch.mybatis.props.MybatisFlexProperties;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.keygen.KeyGeneratorFactory;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class FlexGlobalConfigurer implements MyBatisFlexCustomizer {

    private MybatisFlexProperties properties;

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        this.keyGenerator(globalConfig);
    }

    /**
     * 全局主键生成配置
     */
    private void keyGenerator(FlexGlobalConfig globalConfig) {
        if (properties.getKey().isEnabled()) {
            String name = properties.getKey().getName();
            //注册主键生成器
            KeyGeneratorFactory.register(name, new WorkerIdGenerator());
            //设置主键生成为全局配置
            FlexGlobalConfig.KeyConfig keyConfig = new FlexGlobalConfig.KeyConfig();
            keyConfig.setKeyType(KeyType.Generator);
            keyConfig.setValue(name);
            keyConfig.setBefore(true);
            globalConfig.setKeyConfig(keyConfig);
        }
    }

}
