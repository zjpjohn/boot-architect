package com.cloud.arch.hotkey.net.admin;

import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.config.WorkerServerScheduler;
import com.google.inject.Inject;
import com.ibm.etcd.api.KeyValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

@Slf4j
public class AdminClientWatcher {

    private final AdminClient           adminClient;
    private final IConfigCenter         configCenter;
    private final WorkerServerScheduler scheduler;

    @Inject
    public AdminClientWatcher(IConfigCenter configCenter, WorkerServerScheduler scheduler) {
        this.adminClient  = new AdminClient();
        this.configCenter = configCenter;
        this.scheduler    = scheduler;
    }

    /**
     * 连接并定期拉取admin配置重连
     */
    public void connectAndSchedule() {
        scheduler.schedule(0, 30, () -> {
            try {
                final List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.dashboardPath);
                if (CollectionUtils.isEmpty(keyValues)) {
                    log.warn("admin address is null, please check admin is started.");
                    return;
                }
                final String address = keyValues.get(0).getValue().toStringUtf8();
                adminClient.connect(address);
            } catch (Exception error) {
                log.error(error.getMessage(), error);
            }
        });
    }

}
