package com.cloud.arch.hotkey.net.filter.impl;

import cn.hutool.core.date.SystemClock;
import com.cloud.arch.hotkey.config.WhiteListManager;
import com.cloud.arch.hotkey.key.HotkeyDispatcher;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.net.filter.ICommandFilter;
import com.cloud.arch.hotkey.net.server.WorkerKeyReporter;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.List;

@Slf4j
public class HotKeyCommandFilter implements ICommandFilter {

    private final HotkeyDispatcher  dispatcher;
    private final WorkerKeyReporter reporter;
    private final WhiteListManager  whiteListManager;

    @Inject
    public HotKeyCommandFilter(HotkeyDispatcher dispatcher,
                               WorkerKeyReporter reporter,
                               WhiteListManager whiteListManager) {
        this.dispatcher       = dispatcher;
        this.reporter         = reporter;
        this.whiteListManager = whiteListManager;
    }

    @Override
    public void handle(HotkeyCommand command, ChannelHandlerContext context) {
        reporter.incrReceive();
        List<HotKeyModel> models = command.getHotKeyModels();
        if (CollectionUtils.isEmpty(models)) {
            return;
        }
        long now = SystemClock.now();
        for (HotKeyModel model : models) {
            if (whiteListManager.contains(command.getAppName(), model.getKey())) {
                return;
            }
            //丢弃掉超过1秒的热key事件
            if (now - model.getGmtCreate() <= 1000) {
                dispatcher.dispatch(model, now);
            }
        }
    }

}
