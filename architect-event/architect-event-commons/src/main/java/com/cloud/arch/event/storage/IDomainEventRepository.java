package com.cloud.arch.event.storage;


import java.time.Duration;
import java.util.Collections;
import java.util.List;

public interface IDomainEventRepository {

    void initialize(List<PublishEventEntity> events);

    void markSucceeded(PublishEventEntity entity);

    void markFailed(PublishEventEntity entity, Throwable throwable);

    List<PublishEventEntity> queryFailed(int limit, int maxVersion, Duration before, Duration range);

    default void compensate(EventCompensateEntity entity) {
    }

    /**
     * 查询待移入死信的候选事件（version >= maxVersion）。
     */
    default List<PublishEventEntity> deadEventCandidates(int limit, int maxVersion, Duration before, Duration range) {
        return Collections.emptyList();
    }

    /**
     * 将事件从事件表移入死信表。
     */
    default void archiveDeadEvent(PublishEventEntity entity, String reason) {
    }

    /**
     * 清理超过保留期的死信记录，返回清理条数。
     */
    default int cleanDeadEvents(Duration before) {
        return 0;
    }

    default void batchMarkSucceeded(List<PublishEventEntity> entities) {
        if (!entities.isEmpty()) {
            entities.forEach(this::markSucceeded);
        }
    }

    default void batchMarkFailed(List<PublishEventEntity> entities, Throwable throwable) {
        if (!entities.isEmpty()) {
            entities.forEach(e -> this.markFailed(e, throwable));
        }
    }

    default void checkAffected(PublishEventEntity entity, int affected) {
        if (affected == 0) {
            String errMsg = String.format("Publish [%s] mark [%d]@[%d] to status [%s] error.",
                    entity.getName(),
                    entity.getId(),
                    entity.getVersion(),
                    entity.getState().getState());
            throw new ConcurrentVersionConflictException(errMsg, entity);
        }
    }
}
