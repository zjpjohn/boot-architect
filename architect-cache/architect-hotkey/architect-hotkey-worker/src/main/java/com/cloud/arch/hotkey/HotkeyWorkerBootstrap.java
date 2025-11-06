package com.cloud.arch.hotkey;

import com.cloud.arch.hotkey.dependency.WorkerConfigModule;
import com.cloud.arch.hotkey.dependency.WorkerServerModule;
import com.cloud.arch.hotkey.dependency.WorkerSettingModule;
import com.cloud.arch.hotkey.net.server.HotkeyWorkerServer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class HotkeyWorkerBootstrap {

    private HotkeyWorkerServer hotkeyServer;

    public void start() {
        Injector injector
                = Guice.createInjector(new WorkerSettingModule(), new WorkerConfigModule(), new WorkerServerModule());
        hotkeyServer = injector.getInstance(HotkeyWorkerServer.class);
        Thread shutdownHook = new Thread(() -> {
            try {
                hotkeyServer.close();
            } catch (IOException e) {
                log.error("fatal error during server shutdown. prepare to shalt:", e);
                Runtime.getRuntime().halt(1);
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        hotkeyServer.startServer();
    }

    public void awaitShutdown() {
        if (hotkeyServer != null) {
            try {
                hotkeyServer.awaitShutdown();
            } catch (InterruptedException e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    public static void main(String[] args) {
        HotkeyWorkerBootstrap bootstrap = new HotkeyWorkerBootstrap();
        bootstrap.start();
        bootstrap.awaitShutdown();
    }
}
