package com.cloud.arch.repository;


import com.cloud.arch.core.LogPageQuery;
import com.cloud.arch.core.LogRecord;
import com.cloud.arch.page.Page;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcLogRepository implements ILogRepository {

    private static final String INSERT_LOG_SQL   =
            "insert into arch_biz_log(id,app,tenant,biz_group,biz_no,operator_id,operator,op_action,fail,detail,gmt_create) "
            + "values(:id,:app,:tenant,:biz_group,:biz_no,:operator_id,:operator,:op_action,:fail,:detail,:gmt_create)";
    private static final String QUERY_BIZ_NO_SQL =
            "select id,app,tenant,biz_group,biz_no,operator_id,operator,op_action,fail,detail,gmt_create "
            + "from arch_biz_log where biz_no=:biz_no order by gmt_create desc ";
    private static final String COUNT_QUERY_SQL  = "select count(1) from arch_biz_log ";
    private static final String QUERY_PAGE_SQL   =
            "select id,app,tenant,biz_group,biz_no,operator_id,operator,op_action,fail,detail,gmt_create "
            + "from arch_biz_log ";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcLogRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void save(List<LogRecord> records) {
        jdbcTemplate.batchUpdate(INSERT_LOG_SQL, records.stream()
                                                        .map(e -> new MapSqlParameterSource("id", e.getId()).addValue("app", e.getApp())
                                                                                                            .addValue("tenant", e.getTenant())
                                                                                                            .addValue("biz_group", e.getGroup())
                                                                                                            .addValue("biz_no", e.getBizNo())
                                                                                                            .addValue("operator_id", e.getOperatorId())
                                                                                                            .addValue("operator", e.getOperator())
                                                                                                            .addValue("op_action", e.getAction())
                                                                                                            .addValue("fail", e.getFail())
                                                                                                            .addValue("detail", e.getDetail())
                                                                                                            .addValue("gmt_create", e.getGmtCreate()))
                                                        .toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public List<LogRecord> ofBizNo(String bizNo) {
        MapSqlParameterSource parameter = new MapSqlParameterSource("biz_no", bizNo);
        return jdbcTemplate.query(QUERY_BIZ_NO_SQL, parameter, rowMapper());
    }

    @Override
    public Page<LogRecord> queryPage(LogPageQuery query) {
        Pair<String, MapSqlParameterSource> pair         = buildQuery(query);
        String                              conditionSql = pair.getKey();
        String                              countSql     = COUNT_QUERY_SQL;
        if (!StringUtils.isBlank(conditionSql)) {
            countSql = countSql + " where " + conditionSql;
        }
        Integer         total = jdbcTemplate.queryForObject(countSql, pair.getValue(), Integer.class);
        Page<LogRecord> page  = new Page<>();
        page.setTotal(total);
        page.setPageSize(query.getLimit());
        page.setCurrent(query.getPage());
        if (total > 0) {
            List<LogRecord>       records;
            MapSqlParameterSource parameter = pair.getValue();
            parameter.addValue("offset", (query.getPage() - 1) * query.getLimit());
            parameter.addValue("limit", query.getLimit());
            String orderSql = " order by gmt_create desc limit :offset,:limit";
            String querySql = QUERY_PAGE_SQL;
            if (StringUtils.isBlank(conditionSql)) {
                querySql = querySql + orderSql;
            } else {
                querySql = querySql + " where " + conditionSql + orderSql;
            }
            records = jdbcTemplate.query(querySql, parameter, rowMapper());
            page.setRecords(records);
            page.setSize(records.size());
        }
        return page;
    }

    private RowMapper<LogRecord> rowMapper() {
        return (ResultSet resultSet, int i) -> {
            LogRecord logRecord = new LogRecord();
            logRecord.setId(resultSet.getString("id"));
            logRecord.setApp(resultSet.getString("app"));
            logRecord.setTenant(resultSet.getString("tenant"));
            logRecord.setBizNo(resultSet.getString("biz_no"));
            logRecord.setGroup(resultSet.getString("biz_group"));
            logRecord.setOperator(resultSet.getString("operator_id"));
            logRecord.setOperator(resultSet.getString("operator"));
            logRecord.setAction(resultSet.getString("op_action"));
            logRecord.setFail(resultSet.getInt("fail"));
            logRecord.setDetail(resultSet.getString("detail"));
            logRecord.setGmtCreate(resultSet.getObject("gmt_create", LocalDateTime.class));
            return logRecord;
        };
    }

    public Pair<String, MapSqlParameterSource> buildQuery(LogPageQuery query) {
        MapSqlParameterSource parameter     = new MapSqlParameterSource();
        List<String>          conditionList = Lists.newArrayList();
        if (StringUtils.isNotBlank(query.getApp())) {
            conditionList.add("app=:app");
            parameter.addValue("app", query.getApp());
        }
        if (StringUtils.isNotBlank(query.getGroup())) {
            conditionList.add("biz_group=:biz_group");
            parameter.addValue("biz_group", query.getGroup());
        }
        if (StringUtils.isNotBlank(query.getBizNo())) {
            conditionList.add("biz_no=:biz_no");
            parameter.addValue("biz_no", query.getBizNo());
        }
        if (StringUtils.isNotBlank(query.getTenant())) {
            conditionList.add("tenant=:tenant");
            parameter.addValue("tenant", query.getTenant());
        }
        if (query.getOperatorId() != null) {
            conditionList.add("operator_id=:operator_id");
            parameter.addValue("operator_id", query.getOperatorId());
        }
        String condition = "";
        if (!conditionList.isEmpty()) {
            condition = String.join(" and ", conditionList);
        }
        return Pair.of(condition, parameter);
    }

}
