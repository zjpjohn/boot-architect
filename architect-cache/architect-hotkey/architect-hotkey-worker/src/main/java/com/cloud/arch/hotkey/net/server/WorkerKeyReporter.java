package com.cloud.arch.hotkey.net.server;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.hotkey.cache.CacheManager;
import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.config.WorkerServerScheduler;
import com.cloud.arch.hotkey.config.props.HotKeyProperties;
import com.cloud.arch.hotkey.net.model.AppInfo;
import com.cloud.arch.hotkey.net.model.TotalCount;
import com.cloud.arch.hotkey.utils.IpUtils;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class WorkerKeyReporter {

    /**
     * 用来存储临时收到的key总量，来判断是否很久都没收到key了
     */
    private          long                  tempTotalReceiveKeyCount;
    /**
     * 每次10秒没收到key发过来，就将这个加1，加到3时，就停止自己注册etcd 30秒
     */
    private          int                   mayBeErrorTimes   = 0;
    /**
     * 是否可以继续上报自己的ip
     */
    private volatile boolean               canUpload         = true;
    private final    AtomicLong            totalReceiveCount = new AtomicLong(0L);
    private final    LongAdder             expireTotalCount  = new LongAdder();
    private final    LongAdder             totalDealCount    = new LongAdder();
    private final    LongAdder             totalOfferCount   = new LongAdder();
    private final    WorkerServerScheduler scheduler;
    private final    IConfigCenter         configCenter;
    private final    CacheManager          cacheManager;
    private final    HotKeyProperties      properties;
    private final    HotkeyClientHolder    hotkeyClientHolder;

    @Inject
    public WorkerKeyReporter(WorkerServerScheduler scheduler,
                             IConfigCenter configCenter,
                             CacheManager cacheManager,
                             HotKeyProperties properties,
                             HotkeyClientHolder hotkeyClientHolder) {
        this.scheduler          = scheduler;
        this.configCenter       = configCenter;
        this.cacheManager       = cacheManager;
        this.properties         = properties;
        this.hotkeyClientHolder = hotkeyClientHolder;
    }

    public void incrDeal() {
        this.totalDealCount.increment();
    }

    public void incrReceive() {
        this.totalReceiveCount.incrementAndGet();
    }

    public void incrExpire() {
        this.expireTotalCount.increment();
    }

    public void incrOffer() {
        this.totalOfferCount.increment();
    }

    public void reset() {
        totalReceiveCount.set(0);
        totalDealCount.reset();
        totalOfferCount.reset();
        expireTotalCount.reset();
    }

    public void scheduleUploadClient() {
        scheduler.schedule(0, 10, () -> {
            String ip = IpUtils.getIp();
            for (AppInfo appInfo : hotkeyClientHolder.getAppList()) {
                String appName = appInfo.getName();
                int    clients = appInfo.size();
                //上报应用在当前机器的连接数量
                configCenter.putAndGrant(ConfigConstant.clientCountPath + appName + "/" + ip, clients + "", 13);
            }
            //上报当前worker缓存容量
            configCenter.putAndGrant(ConfigConstant.caffeineSizePath
                                     + ip, JSON.toJSONString(cacheManager.cacheSize()), 13);
            TotalCount totalCount = new TotalCount(this.totalReceiveCount.get(), totalDealCount.sum());
            String     countJson  = JSON.toJSONString(totalCount);
            configCenter.putAndGrant(ConfigConstant.totalReceiveKeyCount + ip, countJson, 13);
            //开启监控检查
            if (this.properties.isMonitor()) {
                this.checkReceiveKeyCount();
            }
        });
    }

    private void checkReceiveKeyCount() {
        if (tempTotalReceiveKeyCount == totalReceiveCount.get()) {
            if (canUpload) {
                mayBeErrorTimes++;
            }
        } else {
            tempTotalReceiveKeyCount = totalReceiveCount.get();
        }
        if (mayBeErrorTimes >= 6) {
            canUpload = false;
            new Thread(() -> {
                try {
                    Thread.sleep(35000);
                    canUpload = true;
                } catch (InterruptedException ignore) {
                }
            }).start();
            mayBeErrorTimes          = 0;
            tempTotalReceiveKeyCount = 0;
            this.reset();
        }
    }

}
