package com.cloud.arch.mybatis.core;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cloud.arch.mybatis.props.MybatisPlusProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
public class TimeMetaObjectHandler implements MetaObjectHandler {

    private final MybatisPlusProperties properties;

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, properties.getGmtCreate(), LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, properties.getGmtModify(), LocalDateTime::now, LocalDateTime.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, properties.getGmtModify(), LocalDateTime::now, LocalDateTime.class);
    }

}
