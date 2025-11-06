package com.cloud.arch.hotkey.network.worker;

import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.network.netty.NettyClient;
import com.ibm.etcd.api.KeyValue;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class HotKeyWorkerManager {

    //热key探测worker列表
    private final List<HotKeyWorker>       workers = new CopyOnWriteArrayList<>();
    //定时调度器
    private final ScheduledExecutorService scheduler;
    //etcd客户端
    private final IConfigCenter            configCenter;
    //当前应用名称
    private final String                   appName;
    //netty连接客户端
    private final NettyClient              client;

    public HotKeyWorkerManager(String appName, IConfigCenter configCenter) {
        this.appName      = appName;
        this.configCenter = configCenter;
        this.scheduler    = Executors.newScheduledThreadPool(1, new DefaultThreadFactory("worker-server-pool"));
        this.client       = new NettyClient(this);
        //定期发现新worker列表
        this.scheduleDiscovery();
        //定期重连失败的worker
        this.scheduleReconnect();
    }

    public String getAppName() {
        return appName;
    }

    public List<HotKeyWorker> getWorkers() {
        return Collections.unmodifiableList(workers);
    }

    /**
     * 定期去etcd注册中心发现worker
     */
    private void scheduleDiscovery() {
        scheduler.scheduleAtFixedRate(this::discoveryWorkers, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * 定位重试连接失败的worker
     */
    private void scheduleReconnect() {
        scheduler.scheduleAtFixedRate(() -> {
            List<String> connectedWorkers = this.getNonConnectedWorkers();
            if (!CollectionUtils.isEmpty(connectedWorkers)) {
                this.client.connect(connectedWorkers);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 发现拉取worker列表
     * 1.初始发现worker并连接
     * 2.定期去发现并连接
     */
    public void discoveryWorkers() {
        List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.workersPath);
        if (CollectionUtils.isEmpty(keyValues)) {
            log.warn("No hotkey worker provide for calculate hot key.");
            return;
        }
        List<String> addresses = keyValues.stream().map(keyValue -> keyValue.getValue().toStringUtf8()).toList();
        this.mergeAndConnect(addresses);
    }

    /**
     * 判断指定worker是否已经正常连接
     *
     * @param address worker地址
     */
    public boolean hasConnected(String address) {
        return workers.stream().anyMatch(server -> server.getAddress().equals(address) && server.channelActive());
    }

    /**
     * 是否已连上worker
     */
    public boolean hasConnectedWorkers() {
        return workers.stream().anyMatch(HotKeyWorker::channelActive);
    }

    /**
     * 获取未连接成功的worker集合，以供重连
     */
    public List<String> getNonConnectedWorkers() {
        return workers.stream()
                      .filter(server -> !server.channelActive())
                      .map(HotKeyWorker::getAddress)
                      .collect(Collectors.toList());
    }

    /**
     * 根据key选定指定服务机器
     * hash取模实现选择，同一个key所有操作都会定位到一台worker上
     *
     * @param key 热key标识
     */
    public Channel chooseWorker(String key) {
        int size = workers.size();
        if (StringUtils.isBlank(key) || size == 0) {
            return null;
        }
        int index = Math.abs(key.hashCode() % size);
        return workers.get(index).getChannel();
    }

    /**
     * 增加新的worker连接列表
     */
    public synchronized void put(String address, Channel channel) {
        Iterator<HotKeyWorker> iterator = workers.iterator();
        boolean                exist    = false;
        while (iterator.hasNext()) {
            HotKeyWorker workerServer = iterator.next();
            if (workerServer.getAddress().equals(address)) {
                workerServer.setChannel(channel);
                exist = true;
                break;
            }
        }
        if (!exist) {
            workers.add(new HotKeyWorker(address, channel));
        }
    }

    /**
     * 删除断线的worker
     *
     * @param address worker地址
     */
    public void removeInactiveWorker(String address) {
        workers.removeIf(server -> server.getAddress().equals(address));
    }

    /**
     * 与本地已经连接的worker进行合并，并连接新的worker机器
     *
     * @param addresses worker机器地址集合
     */
    public void mergeAndConnect(List<String> addresses) {
        //清除无用链接worker
        removeNoneUsed(addresses);
        //连接新的机器
        List<String> newWorkers = newWorkers(addresses);
        if (CollectionUtils.isEmpty(newWorkers)) {
            return;
        }
        client.connect(newWorkers);
        //对机器列表进行排序
        Collections.sort(workers);
    }

    /**
     * 获取地址集中新的连接集合
     *
     * @param addresses 新的worker地址集合
     */
    public List<String> newWorkers(List<String> addresses) {
        Set<String> sets = workers.stream().map(HotKeyWorker::getAddress).collect(Collectors.toSet());
        return addresses.stream().filter(address -> !sets.contains(address)).toList();
    }

    /**
     * 移除地址集合中的worker列表
     *
     * @param addresses worker地址集合
     */
    public void removeNoneUsed(List<String> addresses) {
        for (HotKeyWorker worker : workers) {
            //已连接的机器不在新地址集合中，删除已连接的机器
            if (!addresses.contains(worker.getAddress())) {
                if (worker.getChannel() != null) {
                    worker.getChannel().close();
                }
                workers.remove(worker);
            }
        }
    }
}
