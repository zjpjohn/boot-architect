package com.cloud.arch.transaction.support;

import com.cloud.arch.transaction.codec.AsyncEventCodec;
import com.cloud.arch.transaction.core.AsyncTxEvent;
import com.cloud.arch.transaction.core.IAsyncTxRepository;
import com.cloud.arch.transaction.utils.AsyncTxState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
public class JdbcAsyncTxRepository implements IAsyncTxRepository {

    public static final String INIT_SQL          =
            "insert into arch_tx_log(id,async_Key,shard_key,data,version,state,max_retry,retry_interval,retries,gmt_create,gmt_modify) "
                    + "values(:id,:async_key,:shard_key,:data,:version,:state,:max_retry,:retry_interval,:retries,current_time,current_time)";
    public static final String MARK_RUNNING_SQL  = "update arch_tx_log set state=2,gmt_modify=current_time where id=:id and shard_key=:shard_key";
    public static final String MARK_SUCCESS_SQL  = "update arch_tx_log set state=3,gmt_modify=current_time where id=:id and shard_key=:shard_key";
    public static final String MARK_FAIL_SQL     = "update arch_tx_log set state=4,retries=:retries,next_time=:next_time,gmt_modify=current_time where id=:id and shard_key=:shard_key";
    public static final String MARK_DEAD_SQL     = "update arch_tx_log set state=5,retries=:retries,gmt_modify=current_time where id=:id and shard_key=:shard_key";
    public static final String QUERY_FAIL_SQL    =
            "select id,async_Key,shard_key,data,version,state,max_retry,retry_interval,retries,next_time,gmt_create,gmt_modify "
                    + "from arch_tx_log where state=4 and retries < max_retry and next_time < :time order by next_time asc limit :limit";
    public static final String READY_RUNNING_SQL =
            "select id,async_Key,shard_key,data,version,state,max_retry,retry_interval,retries,next_time,gmt_create,gmt_modify "
                    + "from arch_tx_log where state in (1,2) and gmt_create < :time order by next_time asc";

    private final AsyncEventCodec            asyncEventCodec;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAsyncTxRepository(DataSource dataSource, AsyncEventCodec asyncEventCodec) {
        this.jdbcTemplate    = new NamedParameterJdbcTemplate(dataSource);
        this.asyncEventCodec = asyncEventCodec;
    }

    @Override
    public void initialize(List<AsyncTxEvent> events) {
        jdbcTemplate.batchUpdate(INIT_SQL, events.stream().map(event -> {
            String encoded = asyncEventCodec.encode(event.getData());
            return new MapSqlParameterSource().addValue("id", event.getId())
                                              .addValue("async_key", event.getAsyncKey())
                                              .addValue("shard_key", event.getShardKey())
                                              .addValue("data", encoded)
                                              .addValue("version", event.getVersion())
                                              .addValue("state", event.getState())
                                              .addValue("max_retry", event.getMaxRetry())
                                              .addValue("retry_interval", event.getRetryInterval())
                                              .addValue("retries", event.getRetries());
        }).toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public List<AsyncTxEvent> loadReadyRunning(Duration before) {
        LocalDateTime         now             = LocalDateTime.now();
        MapSqlParameterSource parameterSource = new MapSqlParameterSource().addValue("time", now.minusSeconds(before.getSeconds()));
        return jdbcTemplate.query(READY_RUNNING_SQL, parameterSource, (rs, rowNum) -> this.convert(rs));
    }

    @Override
    public void markSuccess(AsyncTxEvent event) {
        event.setState(AsyncTxState.SUCCESS);
        MapSqlParameterSource paramSource = new MapSqlParameterSource().addValue("id", event.getId())
                                                                       .addValue("shard_key", event.getShardKey());
        jdbcTemplate.update(MARK_SUCCESS_SQL, paramSource);
    }

    @Override
    public void markFail(AsyncTxEvent event) {
        if (event.isDead()) {
            event.setState(AsyncTxState.DEAD);
            MapSqlParameterSource paramSource = new MapSqlParameterSource().addValue("id", event.getId())
                                                                           .addValue("shard_key", event.getShardKey())
                                                                           .addValue("retries", event.getRetries());
            jdbcTemplate.update(MARK_DEAD_SQL, paramSource);
            return;
        }
        event.setState(AsyncTxState.FAIL);
        event.calcNextTime();
        MapSqlParameterSource paramSource = new MapSqlParameterSource().addValue("id", event.getId())
                                                                       .addValue("shard_key", event.getShardKey())
                                                                       .addValue("retries", event.getRetries())
                                                                       .addValue("next_time", event.getNextTime());
        jdbcTemplate.update(MARK_FAIL_SQL, paramSource);
    }

    @Override
    public void markRunning(AsyncTxEvent event) {
        event.setState(AsyncTxState.RUNNING);
        MapSqlParameterSource paramSource = new MapSqlParameterSource().addValue("id", event.getId())
                                                                       .addValue("shard_key", event.getShardKey());
        jdbcTemplate.update(MARK_RUNNING_SQL, paramSource);
    }

    @Override
    public List<AsyncTxEvent> queryFailed(int limit, Duration range) {
        LocalDateTime now = LocalDateTime.now();
        MapSqlParameterSource parameterSource = new MapSqlParameterSource().addValue("time", now.plusSeconds(range.getSeconds()))
                                                                           .addValue("limit", limit);
        return jdbcTemplate.query(QUERY_FAIL_SQL, parameterSource, (rs, row) -> this.convert(rs));
    }

    private AsyncTxEvent convert(ResultSet rs) throws SQLException {
        AsyncTxEvent event = new AsyncTxEvent();
        event.setId(rs.getLong("id"));
        event.setAsyncKey(rs.getString("async_Key"));
        event.setShardKey(rs.getString("shard_key"));
        event.setRetries(rs.getInt("retries"));
        event.setState(rs.getInt("state"));
        event.setVersion(rs.getString("version"));
        event.setMaxRetry(rs.getInt("max_retry"));
        event.setRetryInterval(rs.getLong("retry_interval"));
        event.setNextTime(rs.getObject("next_time", LocalDateTime.class));
        event.setData(asyncEventCodec.decode(rs.getString("data")));
        return event;
    }
}
