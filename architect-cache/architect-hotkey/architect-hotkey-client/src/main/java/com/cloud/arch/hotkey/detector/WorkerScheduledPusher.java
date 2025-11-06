package com.cloud.arch.hotkey.detector;

import com.cloud.arch.hotkey.core.key.IKeyCollector;
import com.cloud.arch.hotkey.core.key.IKeyPusher;
import com.cloud.arch.hotkey.core.key.KeyHotModel;
import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.network.worker.HotKeyWorkerManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WorkerScheduledPusher implements IKeyPusher {

    private final Long                                      hotInterval;
    private final Long                     countInterval;
    private final HotKeyWorkerManager      workerManager;
    private final ScheduledExecutorService                  scheduler;
    private final IKeyCollector<HotKeyModel, HotKeyModel>   hotKeyCollector;
    private final IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector;

    public WorkerScheduledPusher(Long hotInterval,
                                 Long countInterval,
                                 HotKeyWorkerManager workerManager,
                                 IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                                 IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        this.hotInterval       = hotInterval;
        this.countInterval     = countInterval;
        this.workerManager     = workerManager;
        this.scheduler         = Executors.newScheduledThreadPool(2, new DefaultThreadFactory("hotkey-pusher-pool"));
        this.hotKeyCollector   = hotKeyCollector;
        this.keyCountCollector = keyCountCollector;
    }

    /**
     * 定时推送热key探测信息
     */
    public void schedulePushHotKey() {
        this.scheduler.scheduleAtFixedRate(() -> {
            List<HotKeyModel> models = hotKeyCollector.lockAndGet();
            if (!CollectionUtils.isEmpty(models)) {
                send(workerManager.getAppName(), models);
            }
        }, 0, hotInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 定时推送热key统计信息
     */
    public void schedulePushKeyCount() {
        this.scheduler.scheduleAtFixedRate(() -> {
            List<KeyCountModel> models = keyCountCollector.lockAndGet();
            if (!CollectionUtils.isEmpty(models)) {
                sendCount(workerManager.getAppName(), models);
            }
        }, 0, countInterval, TimeUnit.SECONDS);
    }

    /**
     * 发送待测热key
     *
     * @param appName 应用名称
     * @param models  待测热key数据
     */
    @Override
    public void send(String appName, List<HotKeyModel> models) {
        long                            now    = System.currentTimeMillis();
        Map<Channel, List<HotKeyModel>> groups = Maps.newHashMap();
        for (HotKeyModel model : models) {
            model.setGmtCreate(now);
            //根据key进行hash取模分组发送，同一个key的数据发送到同一台worker上处理
            Channel channel = workerManager.chooseWorker(model.getKey());
            if (channel == null) {
                continue;
            }
            List<HotKeyModel> modelList = groups.computeIfAbsent(channel, v -> Lists.newArrayList());
            modelList.add(model);
        }
        for (Channel channel : groups.keySet()) {
            try {
                List<HotKeyModel> keyModels = groups.get(channel);
                HotkeyCommand     command   = new HotkeyCommand(appName, MessageType.REQUEST_NEW_KEY);
                command.setHotKeyModels(keyModels);
                channel.writeAndFlush(command).sync();
            } catch (Exception error) {
                InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
                log.error("push hot key to address:[{}:{}] error:", socketAddress.getHostName(), socketAddress.getPort(), error);
            }
        }
    }

    /**
     * 发送热key统计数据
     *
     * @param appName 应用名称
     * @param models  热key统计数据
     */
    @Override
    public void sendCount(String appName, List<KeyCountModel> models) {
        long                              now    = System.currentTimeMillis();
        Map<Channel, List<KeyCountModel>> groups = Maps.newHashMap();
        for (KeyCountModel model : models) {
            model.setCreateTime(now);
            Channel channel = workerManager.chooseWorker(model.getRuleKey());
            if (channel == null) {
                continue;
            }
            List<KeyCountModel> modelList = groups.computeIfAbsent(channel, v -> Lists.newArrayList());
            modelList.add(model);
        }
        for (Channel channel : groups.keySet()) {
            try {
                List<KeyCountModel> countModels = groups.get(channel);
                HotkeyCommand       command     = new HotkeyCommand(appName, MessageType.REQUEST_HIT_COUNT);
                command.setKeyCountModels(countModels);
                channel.writeAndFlush(command).sync();
            } catch (Exception error) {
                InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
                log.error("push key count to address:[{}:{}] error:", socketAddress.getHostName(), socketAddress.getPort(), error);
            }
        }
    }

}
