package com.cloud.arch.hotkey.config;

import com.cloud.arch.hotkey.config.props.EtcdServerProperties;
import com.cloud.arch.hotkey.config.props.HotKeyProperties;
import com.cloud.arch.hotkey.config.props.WorkerNetProperties;
import com.cloud.arch.hotkey.config.props.WorkerServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

@Slf4j
public class WorkerSettingLoader {

    private final WorkerServerProperties settings;

    private static final WorkerSettingLoader LOADER = new WorkerSettingLoader();

    private WorkerSettingLoader() {
        try {
            InputStream inputStream = WorkerSettingLoader.class.getClassLoader().getResourceAsStream("application.yml");
            Yaml        yaml        = new Yaml();
            this.settings = yaml.loadAs(inputStream, WorkerServerProperties.class);
        } catch (Exception e) {
            log.error("加载应用配置信息异常:", e);
            throw new RuntimeException(e);
        }
    }

    public static WorkerNetProperties getNetwork() {
        return LOADER.settings.getNetwork();
    }

    public static EtcdServerProperties getEtcd() {
        return LOADER.settings.getEtcd();
    }

    public static HotKeyProperties getHotKey() {
        return LOADER.settings.getHotKey();
    }

}
