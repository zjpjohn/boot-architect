package com.cloud.arch.operate.infrast.repository;

import com.cloud.arch.operate.core.OperateType;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.page.PageCondition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class LogRepository {

    private static final String LOG_SQL   = "select "
            + "id,app_no,biz_group,title,type,target,method,req_uri,op_id,op_name,inet_ntoa(op_ip) as op_ip,op_location,state,params,error,taken_time,gmt_create "
            + "from sys_oper_log ";
    private static final String COUNT_SQL = "select count(1) from sys_oper_log ";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LogRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public OperationLog query(Long id) {
        String                whereSql = "where id=:id";
        MapSqlParameterSource source   = new MapSqlParameterSource("id", id);
        return jdbcTemplate.query(LOG_SQL + whereSql, source, new ResultSetExtractor<OperationLog>() {
            @Override
            public OperationLog extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return mapping(rs);
                }
                return null;
            }
        });
    }

    public Integer count(PageCondition condition) {
        Pair<String, MapSqlParameterSource> pair = condition(condition, true);
        return jdbcTemplate.query(COUNT_SQL + pair.getKey(), pair.getRight(), new ResultSetExtractor<Integer>() {
            @Override
            public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        });
    }

    public List<OperationLog> list(PageCondition condition) {
        Pair<String, MapSqlParameterSource> pair = condition(condition, false);
        return jdbcTemplate.query(LOG_SQL + pair.getKey(), pair.getRight(), (rs, rowNum) -> this.mapping(rs));
    }

    private Pair<String, MapSqlParameterSource> condition(PageCondition query, boolean count) {
        String                whereSql  = "";
        MapSqlParameterSource source    = new MapSqlParameterSource();
        Map<String, Object>   condition = query.getCondition();
        if (condition.get("appNo") != null) {
            whereSql += "app_no=:appNo ";
            source.addValue(":appNo", condition.get("appNo"));
        }
        if (condition.get("bizGroup") != null) {
            whereSql += "and biz_group=:bizGroup ";
            source.addValue(":bizGroup", condition.get("bizGroup"));
        }
        if (condition.get("title") != null) {
            whereSql += "and title=:title ";
            source.addValue(":title", condition.get("title"));
        }
        if (condition.get("state") != null) {
            whereSql += "and state=:state ";
            source.addValue(":state", condition.get("state"));
        }
        if (condition.get("type") != null) {
            whereSql += "and type=:type ";
            source.addValue(":type", condition.get("type"));
        }
        if (condition.get("opId") != null) {
            whereSql += "and op_id=:opId ";
            source.addValue(":opId", condition.get("opId"));
        }
        if (condition.get("start") != null && condition.get("end") != null) {
            whereSql += "and gmt_create between :start and :end ";
            source.addValue(":start", condition.get("start")).addValue(":end", condition.get("end"));
        }
        if (StringUtils.isNotBlank(whereSql)) {
            if (whereSql.startsWith("and")) {
                whereSql = whereSql.replaceFirst("and", "");
            }
            whereSql = " where " + whereSql;
        } else if (!count) {
            source.addValue("limit", query.getLimit()).addValue("offset", query.getOffset());
            whereSql = "order by gmt_create desc limit :offset,:limit";
        }
        return Pair.of(whereSql, source);
    }

    private OperationLog mapping(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.setId(rs.getLong("id"));
        log.setAppNo(rs.getString("app_no"));
        log.setBizGroup(rs.getString("biz_group"));
        log.setTitle(rs.getString("title"));
        log.setType(OperateType.of(rs.getString("type")));
        log.setTarget(rs.getString("target"));
        log.setMethod(rs.getString("method"));
        log.setReqUri(rs.getString("req_uri"));
        log.setOpId(rs.getLong("op_id"));
        log.setOpName(rs.getString("op_name"));
        log.setOpIp(rs.getString("op_ip"));
        log.setOpLocation(rs.getString("op_location"));
        log.setState(rs.getInt("state"));
        log.setParams(rs.getString("params"));
        log.setError(rs.getString("error"));
        log.setError(rs.getString("error"));
        log.setTakenTime(rs.getLong("taken_time"));
        log.setGmtCreate(rs.getObject("gmt_create", LocalDateTime.class));
        return log;
    }
}
