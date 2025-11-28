package com.cloud.arch.mybatis.extend;

import com.mybatisflex.core.query.QueryWrapper;

import java.util.function.Consumer;

public interface Condition extends Consumer<QueryWrapper> {
}
