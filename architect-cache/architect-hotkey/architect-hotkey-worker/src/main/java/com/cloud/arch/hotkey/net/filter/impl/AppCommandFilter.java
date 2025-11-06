package com.cloud.arch.hotkey.net.filter.impl;

import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.net.filter.ICommandFilter;
import com.cloud.arch.hotkey.net.server.HotkeyClientHolder;
import com.cloud.arch.hotkey.utils.NettyIpUtil;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;

public class AppCommandFilter implements ICommandFilter {

    private final HotkeyClientHolder hotkeyClientHolder;

    @Inject
    public AppCommandFilter(HotkeyClientHolder hotkeyClientHolder) {
        this.hotkeyClientHolder = hotkeyClientHolder;
    }

    @Override
    public void handle(HotkeyCommand command, ChannelHandlerContext context) {
        String appName = command.getAppName();
        hotkeyClientHolder.newClient(appName, NettyIpUtil.clientIp(context), context);
    }
}
