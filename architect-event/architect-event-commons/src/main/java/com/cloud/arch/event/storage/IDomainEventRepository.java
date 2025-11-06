package com.cloud.arch.event.storage;


import java.time.Duration;
import java.util.List;

public interface IDomainEventRepository {

    void initialize(List<PublishEventEntity> events);

    void markSucceeded(PublishEventEntity entity);

    void markFailed(PublishEventEntity entity, Throwable throwable);

    List<PublishEventEntity> queryFailed(int limit, int maxVersion, Duration before, Duration range);

    void compensate(EventCompensateEntity entity);

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
