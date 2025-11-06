package com.cloud.arch.mybatis.core;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.cloud.arch.utils.IdWorker;

public class CustomIdGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return IdWorker.nextId();
    }

}
