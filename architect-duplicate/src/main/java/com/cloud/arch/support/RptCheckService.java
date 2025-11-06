package com.cloud.arch.support;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class RptCheckService {

    private final RptCheckRepository repository;

    /**
     * 单字段重复校验校验
     */
    public void check(Object target) {
        repository.check(target);
    }

    /**
     * 带约束字段校验
     * 约束字段:字段名称-字段值
     */
    public void check(Object target, Map<String, Object> constraints) {
        repository.check(target, constraints);
    }

}
