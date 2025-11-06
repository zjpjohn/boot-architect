package com.cloud.arch.hotkey.net.filter.impl;

import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.net.filter.ICommandFilter;
import io.netty.channel.ChannelHandlerContext;

public class HeartCommandFilter implements ICommandFilter {

    @Override
    public void handle(HotkeyCommand command, ChannelHandlerContext context) {
        HotkeyCommand pongCommand = new HotkeyCommand(command.getAppName(), MessageType.PONG);
        context.writeAndFlush(pongCommand);
    }
}
