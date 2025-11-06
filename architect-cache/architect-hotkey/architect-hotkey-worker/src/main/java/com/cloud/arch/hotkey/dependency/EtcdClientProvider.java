package com.cloud.arch.hotkey.dependency;

import com.cloud.arch.hotkey.config.EtcdConfigCenter;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.config.props.EtcdServerProperties;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class EtcdClientProvider implements Provider<IConfigCenter> {

    private final EtcdServerProperties properties;

    @Inject
    public EtcdClientProvider(EtcdServerProperties properties) {
        this.properties = properties;
    }

    @Override
    public IConfigCenter get() {
        return new EtcdConfigCenter(properties.getServers());
    }
}
