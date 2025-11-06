package com.cloud.arch.hotkey.config;

import com.google.protobuf.ByteString;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.api.LeaseGrantResponse;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.ibm.etcd.client.lease.PersistentLease;
import com.ibm.etcd.client.lock.LockClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Getter
public class EtcdConfigCenter implements IConfigCenter {

    private final KvClient    kvClient;
    private final LeaseClient leaseClient;
    private final LockClient  lockClient;

    public EtcdConfigCenter(List<String> endpoints) {
        this(EtcdClient.forEndpoints(endpoints).withPlainText().build());
    }

    public EtcdConfigCenter(String endpoint) {
        this(EtcdClient.forEndpoints(endpoint).withPlainText().build());
    }

    public EtcdConfigCenter(KvStoreClient kvStoreClient) {
        this.kvClient    = kvStoreClient.getKvClient();
        this.leaseClient = kvStoreClient.getLeaseClient();
        this.lockClient  = kvStoreClient.getLockClient();
    }

    @Override
    public void put(String key, String value) {
        kvClient.put(ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value)).sync();
    }

    @Override
    public void put(String key, String value, long leaseId) {
        kvClient.put(ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value), leaseId).sync();
    }

    @Override
    public void revoke(long leaseId) {
        leaseClient.revoke(leaseId);
    }

    @Override
    public long putAndGrant(String key, String value, long ttl) {
        LeaseGrantResponse lease = leaseClient.grant(ttl).sync();
        put(key, value, lease.getID());
        return lease.getID();
    }

    @Override
    public long setLease(String key, long leaseId) {
        kvClient.setLease(ByteString.copyFromUtf8(key), leaseId);
        return leaseId;
    }

    @Override
    public void delete(String key) {
        kvClient.delete(ByteString.copyFromUtf8(key)).sync();
    }

    @Override
    public String get(String key) {
        RangeResponse  rangeResponse = kvClient.get(ByteString.copyFromUtf8(key)).sync();
        List<KeyValue> keyValues     = rangeResponse.getKvsList();
        if (keyValues.isEmpty()) {
            return null;
        }
        return keyValues.get(0).getValue().toStringUtf8();
    }

    @Override
    public KeyValue getKv(String key) {
        RangeResponse  rangeResponse = kvClient.get(ByteString.copyFromUtf8(key)).sync();
        List<KeyValue> keyValues     = rangeResponse.getKvsList();
        if (keyValues.isEmpty()) {
            return null;
        }
        return keyValues.get(0);
    }

    @Override
    public List<KeyValue> getPrefix(String key) {
        RangeResponse rangeResponse = kvClient.get(ByteString.copyFromUtf8(key)).asPrefix().sync();
        return rangeResponse.getKvsList();
    }

    @Override
    public KvClient.WatchIterator watch(String key) {
        return kvClient.watch(ByteString.copyFromUtf8(key)).start();
    }

    @Override
    public KvClient.WatchIterator watchPrefix(String key) {
        return kvClient.watch(ByteString.copyFromUtf8(key)).asPrefix().start();
    }

    @Override
    public long keepAlive(String key, String value, int frequencySecs, int minTtl) throws Exception {
        //minTtl秒租期，每frequencySecs秒续约一下
        try (PersistentLease lease = leaseClient.maintain()
                                                .leaseId(System.currentTimeMillis())
                                                .keepAliveFreq(frequencySecs)
                                                .minTtl(minTtl)
                                                .start()) {
            long newId = lease.get(3L, SECONDS);
            put(key, value, newId);
            return newId;
        }
    }

    @Override
    public long buildAliveLease(int frequencySecs, int minTtl) throws Exception {
        try (PersistentLease lease = leaseClient.maintain()
                                                .leaseId(System.currentTimeMillis())
                                                .keepAliveFreq(frequencySecs)
                                                .minTtl(minTtl)
                                                .start()) {
            return lease.get(3L, SECONDS);
        }
    }

    @Override
    public long buildNormalLease(long ttl) {
        LeaseGrantResponse lease = leaseClient.grant(ttl).sync();
        return lease.getID();
    }

    @Override
    public long timeToLive(long leaseId) {
        try {
            return leaseClient.ttl(leaseId).get().getTTL();
        } catch (InterruptedException | ExecutionException error) {
            log.warn("get time to live error:{}", error.getMessage());
            Thread.currentThread().interrupt();
        }
        return 0L;
    }
}
