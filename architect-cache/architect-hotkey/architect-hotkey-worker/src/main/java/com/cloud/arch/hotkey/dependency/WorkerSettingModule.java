package com.cloud.arch.hotkey.dependency;

import com.cloud.arch.hotkey.config.WorkerSettingLoader;
import com.cloud.arch.hotkey.config.props.EtcdServerProperties;
import com.cloud.arch.hotkey.config.props.HotKeyProperties;
import com.cloud.arch.hotkey.config.props.WorkerNetProperties;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;

public class WorkerSettingModule extends AbstractModule {

    @Override
    protected void configure() {
        Binder binder = binder();
        binder.bind(EtcdServerProperties.class).toInstance(WorkerSettingLoader.getEtcd());
        binder.bind(WorkerNetProperties.class).toInstance(WorkerSettingLoader.getNetwork());
        binder.bind(HotKeyProperties.class).toInstance(WorkerSettingLoader.getHotKey());
    }

}
