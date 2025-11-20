package com.cloud.arch.operate.infrast.repository;

import com.cloud.arch.operate.core.OperateType;
import com.cloud.arch.operate.core.OperationLog;
import com.cloud.arch.page.Page;
import com.cloud.arch.page.PageCondition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
            + "id,app_no,tenant_id,biz_group,title,type,target,method,req_uri,op_id,op_name,inet_ntoa(op_ip) as op_ip,op_location,state,params,error,taken_time,gmt_create "
            + "from sys_oper_log ";
    private static final String COUNT_SQL = "select count(1) from sys_oper_log ";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LogRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public OperationLog query(Long id) {
        String                whereSql = "where id=:id";
        MapSqlParameterSource source   = new MapSqlParameterSource("id", id);
        return jdbcTemplate.query(LOG_SQL + whereSql, source, rs -> {
            if (rs.next()) {
                return mapping(rs);
            }
            return null;
        });
    }

    public Page<OperationLog> queryList(PageCondition condition) {
        Pair<String, MapSqlParameterSource> where = where(condition);
        Integer                             count = countLogs(where);
        Page<OperationLog>                  page  = new Page<>();
        page.setTotal(count);
        page.setCurrent(condition.getPage());
        page.setPageSize(condition.getLimit());
        if (count > 0) {
            List<OperationLog> logs = logList(condition, where);
            page.setRecords(logs);
            page.setSize(logs.size());
        }
        return page;
    }

    private Integer countLogs(Pair<String, MapSqlParameterSource> where) {
        return jdbcTemplate.query(COUNT_SQL + where.getKey(), where.getRight(), rs -> {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        });
    }

    private List<OperationLog> logList(PageCondition condition, Pair<String, MapSqlParameterSource> where) {
        Pair<String, MapSqlParameterSource> sqlSource = whereWithPageOrder(condition, where);
        return jdbcTemplate.query(LOG_SQL + sqlSource.getKey(), sqlSource.getRight(), (rs, rowNum) -> this.mapping(rs));
    }

    private Pair<String, MapSqlParameterSource> where(PageCondition query) {
        String                whereSql  = "";
        MapSqlParameterSource source    = new MapSqlParameterSource();
        Map<String, Object>   condition = query.getCondition();
        if (condition.get("tenantId") != null) {
            whereSql += "and tenant_id=:tenantId ";
            source.addValue("tenantId", condition.get("tenantId"));
        }
        if (condition.get("opId") != null) {
            whereSql += "and op_id=:opId ";
            source.addValue("opId", condition.get("opId"));
        }
        if (condition.get("appNo") != null) {
            whereSql += "app_no=:appNo ";
            source.addValue("appNo", condition.get("appNo"));
        }
        if (condition.get("bizGroup") != null) {
            whereSql += "and biz_group=:bizGroup ";
            source.addValue("bizGroup", condition.get("bizGroup"));
        }
        if (condition.get("title") != null) {
            whereSql += "and title=:title ";
            source.addValue("title", condition.get("title"));
        }
        if (condition.get("state") != null) {
            whereSql += "and state=:state ";
            source.addValue("state", condition.get("state"));
        }
        if (condition.get("type") != null) {
            whereSql += "and type=:type ";
            source.addValue("type", condition.get("type"));
        }
        if (condition.get("start") != null && condition.get("end") != null) {
            whereSql += "and gmt_create between :start and :end ";
            source.addValue("start", condition.get("start")).addValue("end", condition.get("end"));
        }
        if (StringUtils.isNotBlank(whereSql)) {
            if (whereSql.startsWith("and")) {
                whereSql = whereSql.replaceFirst("and", "");
            }
            whereSql = " where " + whereSql;
        }
        return Pair.of(whereSql, source);
    }

    private Pair<String, MapSqlParameterSource> whereWithPageOrder(PageCondition query,
                                                                   Pair<String, MapSqlParameterSource> pair) {
        String                whereSql = pair.getKey();
        MapSqlParameterSource source   = pair.getValue();
        source.addValue("limit", query.getLimit()).addValue("offset", query.getOffset());
        whereSql = whereSql + " order by gmt_create desc limit :offset,:limit";
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
