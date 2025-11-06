package com.cloud.arch.event;

import com.cloud.arch.event.rocksdb.RocksdbStorage;
import com.cloud.arch.event.storage.EventCompensateEntity;
import com.cloud.arch.event.storage.IDomainEventRepository;
import com.cloud.arch.event.storage.PublishEventEntity;

import java.time.Duration;
import java.util.List;

public class RocksDomainEventRepository implements IDomainEventRepository {

    private final RocksdbStorage           rocksdbStorage;
    private final RocksReparationProcessor processor;

    public RocksDomainEventRepository(RocksdbStorage rocksdbStorage, RocksReparationProcessor processor) {
        this.rocksdbStorage = rocksdbStorage;
        this.processor      = processor;
    }

    @Override
    public void initialize(List<PublishEventEntity> events) {
        this.rocksdbStorage.save(events);
    }

    @Override
    public void markSucceeded(PublishEventEntity entity) {
        this.rocksdbStorage.remove(entity);
    }

    @Override
    public void markFailed(PublishEventEntity entity, Throwable throwable) {
        processor.push(entity);
    }

    @Override
    public List<PublishEventEntity> queryFailed(int limit, int maxVersion, Duration before, Duration range) {
        final long millis = System.currentTimeMillis();
        return rocksdbStorage.getEvents(millis - before.toMillis(), limit);
    }

    @Override
    public void compensate(EventCompensateEntity entity) {
    }

}
