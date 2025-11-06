package com.cloud.arch.event.rocksdb;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.event.storage.PublishEventEntity;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RocksdbStorage implements SmartInitializingSingleton, DisposableBean {

    private final DBOptions             DB_OPTIONS               = new DBOptions();
    private final ReadOptions           READ_OPTIONS             = new ReadOptions();
    private final WriteOptions          WRITE_OPTIONS_SYNC       = new WriteOptions();
    private final BloomFilter           BLOOM_FILTER             = new BloomFilter();
    private final BlockBasedTableConfig BLOCK_BASED_TABLE_CONFIG = new BlockBasedTableConfig();
    private final ColumnFamilyOptions   COLUMN_FAMILY_OPTIONS    = new ColumnFamilyOptions();
    private final List<CompressionType> COMPRESSION_TYPES        = Lists.newArrayList();

    private final Map<String, ColumnFamilyHandle> cachedHandles = Maps.newHashMap();
    private final String                          dbPath;
    private       OptimisticTransactionDB         rocksDB;

    static {
        RocksDB.loadLibrary();
    }

    public RocksdbStorage(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * 批量保存事件内容
     */
    public void save(List<PublishEventEntity> entities) {
        final ColumnFamilyHandle messageHandle = messageHandle();
        final ColumnFamilyHandle timeHandle    = timeHandle();
        final byte[] current = String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);
        try (Transaction transaction = rocksDB.beginTransaction(WRITE_OPTIONS_SYNC)) {
            try {
                for (PublishEventEntity entity : entities) {
                    final byte[] eventId = String.valueOf(entity.getId()).getBytes(StandardCharsets.UTF_8);
                    final byte[] value   = JSON.toJSONBytes(entity);
                    transaction.put(messageHandle, eventId, value);
                    transaction.put(timeHandle, eventId, current);
                }
                transaction.commit();
            } catch (RocksDBException error) {
                log.error("rocksdb save publish domain event error:", error);
                try {
                    transaction.rollback();
                } catch (RocksDBException ignored) {
                }
                throw new RuntimeException(error.getMessage(), error);
            }
        }
    }

    /**
     * 删除指定事件
     */
    public void remove(PublishEventEntity entity) {
        final ColumnFamilyHandle messageHandle = messageHandle();
        final ColumnFamilyHandle timeHandle    = timeHandle();
        final byte[]             eventId       = String.valueOf(entity.getId()).getBytes(StandardCharsets.UTF_8);
        try (Transaction transaction = rocksDB.beginTransaction(WRITE_OPTIONS_SYNC)) {
            try {
                transaction.delete(messageHandle, eventId);
                transaction.delete(timeHandle, eventId);
                transaction.commit();
            } catch (RocksDBException error) {
                log.error("rocksdb remove event[{}] error:", entity.getId(), error);
                try {
                    transaction.rollback();
                } catch (RocksDBException ignored) {
                }
            }
        }
    }

    /**
     * 查询指定之间之前的事件集合
     *
     * @param beforeMillis 时间戳
     */
    public List<PublishEventEntity> getEvents(Long beforeMillis, int limit) {
        final List<PublishEventEntity> events        = Lists.newArrayList();
        final ColumnFamilyHandle       messageHandle = messageHandle();
        try {
            int                 count    = 0;
            final RocksIterator iterator = this.rocksDB.newIterator(timeHandle(), READ_OPTIONS);
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                final byte[] eventId   = iterator.key();
                final long   timestamp = Long.parseLong(new String(iterator.value(), StandardCharsets.UTF_8));
                if (timestamp >= beforeMillis) {
                    break;
                }
                final byte[]             data  = this.rocksDB.get(messageHandle, eventId);
                final PublishEventEntity event = JSON.parseObject(data, PublishEventEntity.class);
                events.add(event);
                if (count++ >= limit) {
                    break;
                }
            }
        } catch (RocksDBException error) {
            log.error("rocksdb read event before time[{}] error:", beforeMillis, error);
        }
        return events;
    }

    private ColumnFamilyHandle messageHandle() {
        return cachedHandles.get(RocksDbConstants.EVENT_MESSAGE);
    }

    private ColumnFamilyHandle timeHandle() {
        return this.cachedHandles.get(RocksDbConstants.EVENT_TIME);
    }

    private void initialize() {
        this.DB_OPTIONS.setCreateIfMissing(true).setCreateMissingColumnFamilies(true).setMaxOpenFiles(512)
                       .setRowCache(new LRUCache(256 * SizeUnit.MB, 16, true, 5)).setMaxSubcompactions(10);
        READ_OPTIONS.setPrefixSameAsStart(true);
        WRITE_OPTIONS_SYNC.setSync(true);
        BLOCK_BASED_TABLE_CONFIG.setFilterPolicy(BLOOM_FILTER).setCacheIndexAndFilterBlocks(true)
                                .setPinL0FilterAndIndexBlocksInCache(true);

        COMPRESSION_TYPES.addAll(Arrays.asList(CompressionType.NO_COMPRESSION, CompressionType.NO_COMPRESSION, CompressionType.LZ4_COMPRESSION, CompressionType.LZ4_COMPRESSION, CompressionType.LZ4_COMPRESSION, CompressionType.ZSTD_COMPRESSION, CompressionType.ZSTD_COMPRESSION));

        COLUMN_FAMILY_OPTIONS.setTableFormatConfig(BLOCK_BASED_TABLE_CONFIG).useFixedLengthPrefixExtractor(10)
                             .setWriteBufferSize(128 * SizeUnit.MB).setMaxWriteBufferNumber(10)
                             .setLevel0SlowdownWritesTrigger(30).setLevel0StopWritesTrigger(10)
                             .setCompressionPerLevel(COMPRESSION_TYPES).setTargetFileSizeBase(128 * SizeUnit.MB)
                             .setMaxBytesForLevelBase(256 * SizeUnit.MB).setOptimizeFiltersForHits(true);

        final Stopwatch stopwatch = Stopwatch.createStarted();
        if (!createIfNotExistsDir(new File(this.dbPath))) {
            throw new RuntimeException("Failed to create event storage RocksDb dir.");
        }
        try {
            List<ColumnFamilyDescriptor> cfDescriptors = getCFDescriptors();
            List<ColumnFamilyHandle>     cfHandles     = new ArrayList<>();
            this.rocksDB = OptimisticTransactionDB.open(DB_OPTIONS, dbPath, cfDescriptors, cfHandles);
            cacheCFHandles(cfHandles);
            if (log.isInfoEnabled()) {
                log.info("RocksDB start success,times taken:{}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
        } catch (RocksDBException e) {
            log.error("RocksDB initialize error, ex:", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<ColumnFamilyDescriptor> getCFDescriptors() {
        final List<ColumnFamilyDescriptor> list = Lists.newArrayList();
        list.add(new ColumnFamilyDescriptor(RocksDbConstants.DEFAULT_FAMILY.getBytes(StandardCharsets.UTF_8)));
        list.add(new ColumnFamilyDescriptor(RocksDbConstants.EVENT_MESSAGE.getBytes(StandardCharsets.UTF_8)));
        list.add(new ColumnFamilyDescriptor(RocksDbConstants.EVENT_TIME.getBytes(StandardCharsets.UTF_8)));
        return list;
    }

    private void cacheCFHandles(List<ColumnFamilyHandle> handles) throws RocksDBException {
        if (handles == null || handles.size() == 0) {
            log.error("RocksDB init columnFamilyHandle failure.");
            throw new RocksDBException("init columnFamilyHandle failure");
        }
        for (ColumnFamilyHandle cfHandle : handles) {
            this.cachedHandles.put(new String(cfHandle.getName()), cfHandle);
        }
    }

    private boolean createIfNotExistsDir(File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.initialize();
    }

    @Override
    public void destroy() {
        this.DB_OPTIONS.close();
        this.WRITE_OPTIONS_SYNC.close();
        this.READ_OPTIONS.close();
        this.COLUMN_FAMILY_OPTIONS.close();
        cachedHandles.forEach((key, value) -> value.close());
        Optional.ofNullable(rocksDB).ifPresent(RocksDB::close);
    }

}
