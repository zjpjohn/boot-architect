package com.cloud.arch.support;

import com.cloud.arch.utils.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Map;

public record RptFieldValue(RptMetadata metadata, Object value) {

    /**
     * 构建上sql参数
     */
    public Pair<String, MapSqlParameterSource> buildSql(Map<String, Object> constraint) {
        Map<String, Object>   params    = metadata.checkConstraints(constraint);
        MapSqlParameterSource parameter = new MapSqlParameterSource("value", value);
        if (CollectionUtils.isNotEmpty(params)) {
            params.forEach(parameter::addValue);
        }
        return Pair.of(this.metadata.getSql(), parameter);
    }

}
