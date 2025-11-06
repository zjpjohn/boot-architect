package com.cloud.arch.hotkey.dependency;

import com.cloud.arch.hotkey.counter.KeyCounterProcessor;
import com.cloud.arch.hotkey.key.HotkeyDispatcher;
import com.cloud.arch.hotkey.key.HotkeyProcessor;
import com.cloud.arch.hotkey.key.KeyRuleManager;
import com.cloud.arch.hotkey.net.admin.AdminClientWatcher;
import com.cloud.arch.hotkey.net.filter.ICommandFilter;
import com.cloud.arch.hotkey.net.filter.impl.AppCommandFilter;
import com.cloud.arch.hotkey.net.filter.impl.HeartCommandFilter;
import com.cloud.arch.hotkey.net.filter.impl.HotKeyCommandFilter;
import com.cloud.arch.hotkey.net.filter.impl.KeyCountCommandFilter;
import com.cloud.arch.hotkey.net.push.AdminServerPusher;
import com.cloud.arch.hotkey.net.push.AppServerPusher;
import com.cloud.arch.hotkey.net.server.*;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class WorkerServerModule extends AbstractModule {

    @Override
    protected void configure() {
        Binder binder = binder();
        binder.bind(KeyRuleManager.class).in(Scopes.SINGLETON);
        binder.bind(KeyCounterProcessor.class).in(Scopes.SINGLETON);
        binder.bind(HotkeyClientHolder.class).in(Scopes.SINGLETON);
        binder.bind(AppServerPusher.class).in(Scopes.SINGLETON);
        binder.bind(AdminServerPusher.class).in(Scopes.SINGLETON);
        binder.bind(HotkeyProcessor.class).in(Scopes.SINGLETON);
        binder.bind(HotkeyDispatcher.class).in(Scopes.SINGLETON);
        binder.bind(WorkerRegister.class).in(Scopes.SINGLETON);
        binder.bind(WorkerKeyReporter.class).in(Scopes.SINGLETON);
        binder.bind(ICommandFilter.class).annotatedWith(Names.named("heartBeat")).to(HeartCommandFilter.class)
              .in(Scopes.SINGLETON);
        binder.bind(ICommandFilter.class).annotatedWith(Names.named("app")).to(AppCommandFilter.class)
              .in(Scopes.SINGLETON);
        binder.bind(ICommandFilter.class).annotatedWith(Names.named("keyCount")).to(KeyCountCommandFilter.class)
              .in(Scopes.SINGLETON);
        binder.bind(ICommandFilter.class).annotatedWith(Names.named("hotKey")).to(HotKeyCommandFilter.class)
              .in(Scopes.SINGLETON);
        binder.bind(HotkeyWorkerHandler.class).in(Scopes.SINGLETON);
        binder.bind(AdminClientWatcher.class).in(Scopes.SINGLETON);
        binder.bind(HotkeyWorkerServer.class).in(Scopes.SINGLETON);
    }
}
