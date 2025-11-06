package com.cloud.arch.operate.core;

import java.util.List;
import java.util.Map;

public interface IOperatorResolver {

    Map<Long, String> resolve(List<Long> idList);

}
