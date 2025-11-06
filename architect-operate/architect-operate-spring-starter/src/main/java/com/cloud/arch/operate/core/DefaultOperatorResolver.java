package com.cloud.arch.operate.core;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class DefaultOperatorResolver implements IOperatorResolver {
    @Override
    public Map<Long, String> resolve(List<Long> idList) {
        return Maps.newHashMap();
    }

}
