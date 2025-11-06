package com.cloud.arch.cache.extension;

import com.cloud.arch.cache.props.HotKeyCacheProperties;
import com.cloud.arch.hotkey.core.key.IKeyCollector;
import com.cloud.arch.hotkey.core.key.KeyHotModel;
import com.cloud.arch.hotkey.detector.WorkerScheduledPusher;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.KeyCountModel;
import com.cloud.arch.hotkey.network.worker.HotKeyWorkerManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

public class ScheduledPusherFactoryBean implements FactoryBean<WorkerScheduledPusher>, SmartInitializingSingleton {

    private final WorkerScheduledPusher workerScheduledPusher;

    public ScheduledPusherFactoryBean(HotKeyCacheProperties properties,
                                      HotKeyWorkerManager workerManager,
                                      IKeyCollector<HotKeyModel, HotKeyModel> hotKeyCollector,
                                      IKeyCollector<KeyHotModel, KeyCountModel> keyCountCollector) {
        workerScheduledPusher = new WorkerScheduledPusher(properties.getHotReportInterval(),
                properties.getCountReportInterval(),
                workerManager,
                hotKeyCollector,
                keyCountCollector);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public WorkerScheduledPusher getObject() throws Exception {
        return workerScheduledPusher;
    }

    @Override
    public Class<?> getObjectType() {
        return WorkerScheduledPusher.class;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.workerScheduledPusher.schedulePushHotKey();
        this.workerScheduledPusher.schedulePushKeyCount();
    }

}
