package com.cloud.arch.hotkey.net.server;

import com.cloud.arch.hotkey.config.ConfigConstant;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.config.WorkerServerScheduler;
import com.cloud.arch.hotkey.config.props.WorkerNetProperties;
import com.cloud.arch.hotkey.utils.IpUtils;
import com.google.inject.Inject;

import java.util.Optional;

public class WorkerRegister {

    private final IConfigCenter         configCenter;
    private final WorkerServerScheduler scheduler;
    private final WorkerNetProperties   netProperties;
    private final String                hostname;

    @Inject
    public WorkerRegister(IConfigCenter configCenter,
                          WorkerServerScheduler scheduler,
                          WorkerNetProperties netProperties) {
        this.configCenter  = configCenter;
        this.scheduler     = scheduler;
        this.netProperties = netProperties;
        this.hostname      = IpUtils.getHostName();
    }

    /**
     * 定期上报worker自己状态
     */
    public void scheduleReport() {
        scheduler.schedule(0, 5, () -> {
            String key = ConfigConstant.workersPath + hostname;
            String ip  = Optional.ofNullable(netProperties.getBind()).orElseGet(IpUtils::getIp);
            configCenter.putAndGrant(key, ip + ":" + netProperties.getPort(), 8);
        });
    }

    /**
     * 掉线立即删除自己
     */
    public void detachWorker() {
        configCenter.delete(ConfigConstant.workersPath + hostname);
    }

}
