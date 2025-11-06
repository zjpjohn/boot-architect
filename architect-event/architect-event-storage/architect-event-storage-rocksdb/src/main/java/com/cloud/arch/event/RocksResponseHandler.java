package com.cloud.arch.event;

import com.cloud.arch.event.remoting.RemotingResponseHandler;
import com.cloud.arch.event.reparation.ReparationResponse;
import com.cloud.arch.event.rocksdb.RocksdbStorage;
import com.cloud.arch.event.storage.PublishEventEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RocksResponseHandler implements RemotingResponseHandler {

    private final RocksdbStorage rocksdbStorage;

    public RocksResponseHandler(RocksdbStorage rocksdbStorage) {
        this.rocksdbStorage = rocksdbStorage;
    }

    /**
     * 响应成功处理
     *
     * @param response server端响应内容
     */
    @Override
    public void onHandle(ReparationResponse response) {
        try {
            if (response.isSuccess()) {
                rocksdbStorage.remove(new PublishEventEntity(response.getEventId()));
            }
        } catch (Exception error) {
            log.error("补偿事件响应处理结果异常:", error);
        }
    }
}
