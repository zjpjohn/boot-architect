package com.cloud.arch.support;

import com.cloud.arch.web.error.ApiBizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

@Slf4j
public class RptCheckRepository {

    public static final String REPEAT_SQL            = "select ifnull((select 1 from $tableName where $columnName=:value limit 1),0) as `exist`";
    public static final String REPEAT_CONSTRAINT_SQL = "select ifnull((select 1 from $tableName where $columnName=:value $constraint limit 1),0) as `exist`";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RptCheckRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * 单字段不带约束条件的重复校验
     */
    public void check(Object target) {
        this.check(target, null);
    }

    /**
     * 带约束字段的重复校验
     */
    public void check(Object target, Map<String, Object> constraint) {
        if (noneNull(target)) {
            return;
        }
        List<RptFieldValue> valueList = RptMetadataContainer.compute(target);
        for (RptFieldValue fieldValue : valueList) {
            checkField(fieldValue, constraint);
        }
    }

    /**
     * 单个字段校验
     */
    private void checkField(RptFieldValue value, Map<String, Object> constraint) {
        boolean result = this.queryExist(value, constraint);
        if (result) {
            throw new ApiBizException(HttpStatus.BAD_REQUEST, 400, value.metadata().getMessage());
        }
    }

    /**
     * 执行sql查询是否重复存在
     */
    public boolean queryExist(RptFieldValue value, Map<String, Object> constraint) {
        Pair<String, MapSqlParameterSource> condition = value.buildSql(constraint);
        Integer exist = jdbcTemplate.query(condition.getKey(), condition.getValue(), (ResultSet resultSet) -> {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        });
        return exist != null && exist > 0;
    }

    private boolean noneNull(Object target) {
        if (target instanceof String str) {
            return StringUtils.isNotBlank(str);
        }
        return target != null;
    }

}
