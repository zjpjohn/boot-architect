package com.cloud.arch.operate.repository;

import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.utils.IdWorker;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

public class LogJdbcRepository {

    private static final String INSERT_SQL =
            "insert into sys_oper_log(id,app_no,biz_group,title,type,target,method,req_uri,op_id,op_name,op_ip,op_location,state,params,error,taken_time,gmt_create) "
                    + "values(:id,:appNo,:bizGroup,:title,:type,:target,:method,:reqUri,:opId,:opName,inet_aton(:opIp),:opLocation,:state,:params,:error,:takenTime,:gmtCreate)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LogJdbcRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(List<OperationLog> records) {
        MapSqlParameterSource[] parameterSources = records.stream()
                                                          .map(this::buildParameter)
                                                          .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(INSERT_SQL, parameterSources);
    }

    public MapSqlParameterSource buildParameter(OperationLog log) {
        return new MapSqlParameterSource().addValue("id", IdWorker.nextId())
                                          .addValue("appNo", log.getAppNo())
                                          .addValue("bizGroup", log.getBizGroup())
                                          .addValue("title", log.getTitle())
                                          .addValue("type", log.getType().value())
                                          .addValue("target", log.getTarget())
                                          .addValue("method", log.getMethod())
                                          .addValue("reqUri", log.getReqUri())
                                          .addValue("opId", log.getOpId())
                                          .addValue("opName", log.getOpName())
                                          .addValue("opIp", log.getOpIp())
                                          .addValue("opLocation", log.getOpLocation())
                                          .addValue("state", log.getState())
                                          .addValue("params", log.getParams())
                                          .addValue("error", log.getError())
                                          .addValue("takenTime", log.getTakenTime())
                                          .addValue("gmtCreate", LocalDateTime.now());
    }
}
