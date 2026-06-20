package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.EventState;
import com.cloud.arch.event.storage.EventCompensateEntity;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

public class JdbcDomainEventRepository implements IDomainEventRepository {

    private static final String INITIALIZE_SQL            = "insert into arch_event(id,name,filter,delay,event,shard_key,state,version,gmt_create) values(:id,:name,:filter,:delay,:event,:shard_key,:state,:version,:gmt_create)";
    private static final String MARK_SUCCESS_SQL          = "update arch_event set state=1,version=version+1,publish_time=:publish_time where id=:id and version=:version and shard_key=:shard_key";
    private static final String MARK_FAILED_SQL           = "update arch_event set state=2,version=version+1 where id=:id and version=:version and shard_key=:shard_key";
    private static final String QUERY_FAILED_SQL          =
            "select id,name,filter,delay,event,shard_key,state,version,gmt_create from arch_event " +
            "where state<>1 and gmt_create between :lower and :upper and version<:maxVersion order by version asc limit :limit ";
    private static final String COMPENSATE_SQL            =
            "insert into arch_event_compen(id,event_id,shard_key,start_time,taken,fail_msg,gmt_create) " +
            "values(:id,:event_id,:shard_key,:start_time,:taken,:fail_msg,:gmt_create)";
    private static final String QUERY_DEAD_LETTER_SQL     =
            "select id,name,filter,delay,event,shard_key,state,version,gmt_create from arch_event " +
            "where state<>1 and gmt_create between :lower and :upper and version>=:maxVersion order by version desc limit :limit ";
    private static final String MOVE_TO_DEAD_LETTER_SQL   =
            "insert into arch_event_dead(id,name,filter,delay,event,shard_key,gmt_create,dead_time,dead_reason) " +
            "values(:id,:name,:filter,:delay,:event,:shard_key,:gmt_create,:dead_time,:dead_reason)";
    private static final String DELETE_EVENT_SQL          = "delete from arch_event where id=:id and shard_key=:shard_key";
    private static final String CLEAN_DEAD_LETTER_SQL     = "delete from arch_event_dead where dead_time<:before limit :limit";
    private static final String CLEAN_SUCCEEDED_EVENT_SQL = "delete from arch_event where state=1 and gmt_create<:before limit :limit";
    private static final String BATCH_MARK_SUCCESS_SQL    = "update arch_event set state=1, version=version+1, publish_time=:publish_time where id=:id and version=:version and shard_key=:shard_key";
    private static final String BATCH_MARK_FAILED_SQL     = "update arch_event set state=2, version=version+1 where id=:id and version=:version and shard_key=:shard_key";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate        transactionTemplate;

    public JdbcDomainEventRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public void initialize(List<PublishEventEntity> events) {
        jdbcTemplate.batchUpdate(INITIALIZE_SQL, events.stream()
                                                       .map(event -> new MapSqlParameterSource().addValue("id", event.getId())
                                                                                                .addValue("name", event.getName())
                                                                                                .addValue("filter", event.getFilter())
                                                                                                .addValue("delay", event.getDelay())
                                                                                                .addValue("event", event.getEvent())
                                                                                                .addValue("shard_key", event.getShardingKey())
                                                                                                .addValue("state", event.getEventState())
                                                                                                .addValue("version", event.getVersion())
                                                                                                .addValue("gmt_create", event.getGmtCreate()))
                                                       .toArray(MapSqlParameterSource[]::new));
    }

    @Override
    public void markSucceeded(PublishEventEntity entity) {
        MapSqlParameterSource parameter = new MapSqlParameterSource("id", entity.getId());
        parameter.addValue("version", entity.getVersion())
                 .addValue("publish_time", System.currentTimeMillis())
                 .addValue("shard_key", entity.getShardingKey());
        int affected = jdbcTemplate.update(MARK_SUCCESS_SQL, parameter);
        checkAffected(entity, affected);
    }

    @Override
    public void markFailed(PublishEventEntity entity, Throwable throwable) {
        MapSqlParameterSource parameter = new MapSqlParameterSource("id", entity.getId());
        parameter.addValue("version", entity.getVersion()).addValue("shard_key", entity.getShardingKey());
        int affected = jdbcTemplate.update(MARK_FAILED_SQL, parameter);
        checkAffected(entity, affected);
    }

    @Override
    public void batchMarkSucceeded(List<PublishEventEntity> entities) {
        long now = System.currentTimeMillis();
        MapSqlParameterSource[] params = entities.stream()
                                                 .map(e -> new MapSqlParameterSource("id", e.getId()).addValue("version", e.getVersion())
                                                                                                     .addValue("shard_key", e.getShardingKey())
                                                                                                     .addValue("publish_time", now))
                                                 .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(BATCH_MARK_SUCCESS_SQL, params);
    }

    @Override
    public void batchMarkFailed(List<PublishEventEntity> entities, Throwable throwable) {
        MapSqlParameterSource[] params = entities.stream()
                                                 .map(e -> new MapSqlParameterSource("id", e.getId()).addValue("version", e.getVersion())
                                                                                                     .addValue("shard_key", e.getShardingKey()))
                                                 .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(BATCH_MARK_FAILED_SQL, params);
    }

    @Override
    public List<PublishEventEntity> queryFailed(int limit, int maxVersion, Duration before, Duration range) {
        final long            current   = System.currentTimeMillis();
        MapSqlParameterSource parameter = new MapSqlParameterSource("limit", limit);
        parameter.addValue("lower", current - range.toMillis())
                 .addValue("upper", current - before.toMillis())
                 .addValue("maxVersion", maxVersion);
        return jdbcTemplate.query(QUERY_FAILED_SQL, parameter, (rs, rowNum) -> {
            PublishEventEntity entity = new PublishEventEntity();
            entity.setId(rs.getLong("id"));
            entity.setName(rs.getString("name"));
            entity.setFilter(rs.getString("filter"));
            entity.setDelay(rs.getLong("delay"));
            entity.setEvent(rs.getString("event"));
            entity.setShardingKey(rs.getString("shard_key"));
            entity.setState(EventState.of(rs.getInt("state")));
            entity.setVersion(rs.getInt("version"));
            entity.setGmtCreate(rs.getLong("gmt_create"));
            return entity;
        });
    }

    @Override
    public void compensate(EventCompensateEntity entity) {
        MapSqlParameterSource parameter = new MapSqlParameterSource("id", entity.getId());
        parameter.addValue("event_id", entity.getEventId())
                 .addValue("start_time", entity.getStartTime())
                 .addValue("shard_key", entity.getShardingKey())
                 .addValue("taken", entity.getTaken())
                 .addValue("fail_msg", entity.getFailedMsg())
                 .addValue("gmt_create", entity.getGmtCreate());
        jdbcTemplate.update(COMPENSATE_SQL, parameter);
    }

    @Override
    public void batchCompensate(List<EventCompensateEntity> entities) {
        MapSqlParameterSource[] params = entities.stream()
                                                 .map(e -> new MapSqlParameterSource("id", e.getId())
                                                         .addValue("event_id", e.getEventId())
                                                         .addValue("shard_key", e.getShardingKey())
                                                         .addValue("start_time", e.getStartTime())
                                                         .addValue("taken", e.getTaken())
                                                         .addValue("fail_msg", e.getFailedMsg())
                                                         .addValue("gmt_create", e.getGmtCreate()))
                                                 .toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(COMPENSATE_SQL, params);
    }

    @Override
    public List<PublishEventEntity> deadEventCandidates(int limit, int maxVersion, Duration before, Duration range) {
        final long            current   = System.currentTimeMillis();
        MapSqlParameterSource parameter = new MapSqlParameterSource("limit", limit);
        parameter.addValue("lower", current - range.toMillis())
                 .addValue("upper", current - before.toMillis())
                 .addValue("maxVersion", maxVersion);
        return jdbcTemplate.query(QUERY_DEAD_LETTER_SQL, parameter, (rs, rowNum) -> {
            PublishEventEntity entity = new PublishEventEntity();
            entity.setId(rs.getLong("id"));
            entity.setName(rs.getString("name"));
            entity.setFilter(rs.getString("filter"));
            entity.setDelay(rs.getLong("delay"));
            entity.setEvent(rs.getString("event"));
            entity.setShardingKey(rs.getString("shard_key"));
            entity.setState(EventState.of(rs.getInt("state")));
            entity.setVersion(rs.getInt("version"));
            entity.setGmtCreate(rs.getLong("gmt_create"));
            return entity;
        });
    }

    @Override
    public void archiveDeadEvent(PublishEventEntity entity, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            MapSqlParameterSource param = new MapSqlParameterSource("id", entity.getId());
            param.addValue("name", entity.getName())
                 .addValue("filter", entity.getFilter())
                 .addValue("delay", entity.getDelay())
                 .addValue("event", entity.getEvent())
                 .addValue("shard_key", entity.getShardingKey())
                 .addValue("gmt_create", entity.getGmtCreate())
                 .addValue("dead_time", System.currentTimeMillis())
                 .addValue("dead_reason", reason);
            jdbcTemplate.update(MOVE_TO_DEAD_LETTER_SQL, param);
            jdbcTemplate.update(DELETE_EVENT_SQL, new MapSqlParameterSource("id", entity.getId()).addValue("shard_key", entity.getShardingKey()));
        });
    }

    @Override
    public int cleanSucceededEvents(long beforeMillis, int limit) {
        MapSqlParameterSource param = new MapSqlParameterSource("before", beforeMillis).addValue("limit", limit);
        return jdbcTemplate.update(CLEAN_SUCCEEDED_EVENT_SQL, param);
    }

    @Override
    public int cleanDeadEvents(Duration before) {
        MapSqlParameterSource param = new MapSqlParameterSource("before", System.currentTimeMillis() -
                                                                          before.toMillis()).addValue("limit", 200);
        return jdbcTemplate.update(CLEAN_DEAD_LETTER_SQL, param);
    }

}
