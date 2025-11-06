package com.cloud.arch.hotkey.net.server;

import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.net.filter.ICommandFilter;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
@ChannelHandler.Sharable
public class HotkeyWorkerHandler extends SimpleChannelInboundHandler<HotkeyCommand> {

    private final Map<MessageType, ICommandFilter> filterMap = Maps.newHashMap();

    private final HotkeyClientHolder hotkeyClientHolder;

    @Inject
    public HotkeyWorkerHandler(HotkeyClientHolder hotkeyClientHolder,
                               @Named("app") ICommandFilter appCommandFilter,
                               @Named("heartBeat") ICommandFilter heartCommandFilter,
                               @Named("hotKey") ICommandFilter hotKeyCommandFilter,
                               @Named("keyCount") ICommandFilter keyCountCommandFilter) {
        this.hotkeyClientHolder = hotkeyClientHolder;
        filterMap.put(MessageType.APP_NAME, appCommandFilter);
        filterMap.put(MessageType.PING, heartCommandFilter);
        filterMap.put(MessageType.REQUEST_NEW_KEY, hotKeyCommandFilter);
        filterMap.put(MessageType.REQUEST_HIT_COUNT, keyCountCommandFilter);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        hotkeyClientHolder.loseClient(ctx);
        ctx.close();
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, HotkeyCommand command) throws Exception {
        if (command == null) {
            return;
        }
        Optional.ofNullable(filterMap.get(command.getMessageType())).ifPresent(filer -> filer.handle(command, context));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
