package com.cloud.arch.cache.extension;

import com.cloud.arch.hotkey.config.IConfigCenter;
import com.cloud.arch.hotkey.core.rule.KeyRuleManager;
import com.cloud.arch.hotkey.detector.HotKeyDetectWatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

@Slf4j
public class HotKeyWatcherFactoryBean
        implements FactoryBean<HotKeyDetectWatcher>, DisposableBean, SmartInitializingSingleton {

    private final HotKeyDetectWatcher hotKeyDetectWatcher;

    public HotKeyWatcherFactoryBean(String appName, IConfigCenter configCenter, KeyRuleManager keyRuleManager) {
        this.hotKeyDetectWatcher = new HotKeyDetectWatcher(appName, configCenter, keyRuleManager);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public HotKeyDetectWatcher getObject() throws Exception {
        return hotKeyDetectWatcher;
    }


    @Override
    public Class<?> getObjectType() {
        return HotKeyDetectWatcher.class;
    }

    @Override
    public void destroy() throws Exception {
        this.hotKeyDetectWatcher.dispose();
    }

    @Override
    public void afterSingletonsInstantiated() {
        hotKeyDetectWatcher.initialize();
    }

}
