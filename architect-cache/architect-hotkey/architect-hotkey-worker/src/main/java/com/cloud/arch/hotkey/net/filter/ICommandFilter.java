package com.cloud.arch.hotkey.net.filter;

import com.cloud.arch.hotkey.model.HotkeyCommand;
import io.netty.channel.ChannelHandlerContext;

public interface ICommandFilter {

    void handle(HotkeyCommand command, ChannelHandlerContext context);

}
