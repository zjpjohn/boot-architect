package com.cloud.arch.event;

import com.cloud.arch.event.core.publish.EventState;
import com.cloud.arch.event.storage.EventCompensateEntity;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

public class JdbcDomainEventRepository implements IDomainEventRepository {

    private static final String INITIALIZE_SQL   = "insert into arch_event(id,name,filter,delay,event,shard_key,state,version,gmt_create) values(:id,:name,:filter,:delay,:event,:shard_key,:state,:version,:gmt_create)";
    private static final String MARK_SUCCESS_SQL = "update arch_event set state=1,version=version+1,publish_time=:publish_time where id=:id and version=:version";
    private static final String MARK_FAILED_SQL  = "update arch_event set state=2,version=version+1 where id=:id and version=:version";
    private static final String QUERY_FAILED_SQL =
            "select id,name,filter,delay,event,shard_key,state,version,gmt_create from arch_event "
                    + "where state<>1 and gmt_create between :lower and :upper and version<:maxVersion order by version asc limit :limit ";
    private static final String COMPENSATE_SQL   =
            "insert into arch_event_compen(id,event_id,shard_key,start_time,taken,fail_msg,gmt_create) "
                    + "values(:id,:event_id,:shard_key,:start_time,:taken,:fail_msg,:gmt_create)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcDomainEventRepository(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
        MapSqlParameterSource parameter = new MapSqlParameterSource("id", entity.getId()).addValue("version", entity.getVersion())
                                                                                         .addValue("publish_time", System.currentTimeMillis());
        int affected = jdbcTemplate.update(MARK_SUCCESS_SQL, parameter);
        checkAffected(entity, affected);
    }

    @Override
    public void markFailed(PublishEventEntity entity, Throwable throwable) {
        MapSqlParameterSource parameter = new MapSqlParameterSource("id", entity.getId()).addValue("version", entity.getVersion());
        int                   affected  = jdbcTemplate.update(MARK_FAILED_SQL, parameter);
        checkAffected(entity, affected);
    }

    @Override
    public List<PublishEventEntity> queryFailed(int limit, int maxVersion, Duration before, Duration range) {
        final long current = System.currentTimeMillis();
        MapSqlParameterSource parameter = new MapSqlParameterSource("limit", limit).addValue("lower", current
                - range.toMillis()).addValue("upper", current - before.toMillis()).addValue("maxVersion", maxVersion);
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
        MapSqlParameterSource parameter = new MapSqlParameterSource("id", entity.getId()).addValue("event_id", entity.getEventId())
                                                                                         .addValue("start_time", entity.getStartTime())
                                                                                         .addValue("shard_key", entity.getShardingKey())
                                                                                         .addValue("taken", entity.getTaken())
                                                                                         .addValue("fail_msg", entity.getFailedMsg())
                                                                                         .addValue("gmt_create", entity.getGmtCreate());
        jdbcTemplate.update(COMPENSATE_SQL, parameter);
    }

}
