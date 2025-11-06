package com.cloud.arch.hotkey.dependency;

import com.cloud.arch.hotkey.cache.CacheManager;
import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.config.LoggerManager;
import com.cloud.arch.hotkey.config.WhiteListManager;
import com.cloud.arch.hotkey.config.WorkerServerScheduler;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Scopes;

public class WorkerConfigModule extends AbstractModule {

    @Override
    protected void configure() {
        Binder binder = binder();
        binder.bind(CacheManager.class).in(Scopes.SINGLETON);
        binder.bind(WorkerServerScheduler.class).in(Scopes.SINGLETON);
        binder.bind(IConfigCenter.class).toProvider(EtcdClientProvider.class).in(Scopes.SINGLETON);
        binder.bind(LoggerManager.class).in(Scopes.SINGLETON);
        binder.bind(WhiteListManager.class).in(Scopes.SINGLETON);
    }
}
