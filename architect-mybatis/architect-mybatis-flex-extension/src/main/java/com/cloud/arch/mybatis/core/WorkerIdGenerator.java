package com.cloud.arch.mybatis.core;

import com.cloud.arch.utils.IdWorker;
import com.mybatisflex.core.keygen.IKeyGenerator;

public class WorkerIdGenerator implements IKeyGenerator {

    @Override
    public Object generate(Object entity, String keyColumn) {
        return IdWorker.nextId();
    }

}
