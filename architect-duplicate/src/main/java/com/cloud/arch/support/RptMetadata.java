package com.cloud.arch.support;

import com.cloud.arch.annotation.RptField;
import com.cloud.arch.utils.CollectionUtils;
import com.cloud.arch.web.error.ApiBizException;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloud.arch.support.RptCheckRepository.REPEAT_CONSTRAINT_SQL;
import static com.cloud.arch.support.RptCheckRepository.REPEAT_SQL;

@Getter
public class RptMetadata {

    private final Field        field;
    private final String       table;
    private final String       column;
    private final String       message;
    private final List<String> constraints;
    private final String       sql;

    public RptMetadata(RptField annotation, Field field) {
        this.field  = field;
        this.table  = annotation.table();
        this.column = annotation.column();
        Preconditions.checkState(StringUtils.isNotBlank(this.table), "数据表名为空");
        Preconditions.checkState(StringUtils.isNotBlank(this.column), "数据字段为空");
        this.message     = annotation.message();
        this.constraints = Splitter.on(",").trimResults().splitToList(annotation.constraints());
        this.sql         = this.constraint();
    }

    /**
     * 构建校验重复参数
     */
    public RptFieldValue rptValue(Object target) {
        field.setAccessible(true);
        try {
            Object value = field.get(target);
            if (notNull(value)) {
                return new RptFieldValue(this, value);
            }
            return null;
        } catch (IllegalAccessException error) {
            throw new RuntimeException(error);
        }
    }

    /**
     * 对校验参数进行校验
     */
    public Map<String, Object> checkConstraints(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(this.constraints)) {
            return Collections.emptyMap();
        }
        if (CollectionUtils.isEmpty(params)) {
            throw new ApiBizException(HttpStatus.INTERNAL_SERVER_ERROR, 500, "重复校验配置错误，请确认配置.");
        }
        Map<String, Object> newParams = params.entrySet()
                                              .stream()
                                              .filter(entry -> this.constraints.contains(entry.getKey()))
                                              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (newParams.size() < this.constraints.size()) {
            throw new ApiBizException(HttpStatus.INTERNAL_SERVER_ERROR, 500, "重复校验配置错误，请确认配置.");
        }
        return newParams;
    }

    /**
     * 约束字段sql拼接
     */
    private String constraint() {
        String sql = REPEAT_SQL;
        if (CollectionUtils.isNotEmpty(this.constraints)) {
            String constrainSql = constraints.stream()
                                             .map(key -> key + "=:" + key)
                                             .collect(Collectors.joining(" and ", " and ", " "));
            sql = REPEAT_CONSTRAINT_SQL.replace("$constraint", constrainSql);
        }
        return sql.replace("$tableName", this.table).replace("$columnName", this.column);
    }


    private boolean notNull(Object value) {
        if (value instanceof String target) {
            return StringUtils.isNotBlank(target);
        }
        return value != null;
    }

}
