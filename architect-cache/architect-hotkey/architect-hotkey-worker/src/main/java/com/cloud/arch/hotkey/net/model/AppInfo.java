package com.cloud.arch.hotkey.net.model;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class AppInfo {

    private final String       name;
    private final ChannelGroup channelGroup;

    public AppInfo(String name) {
        this.name         = name;
        this.channelGroup = new DefaultChannelGroup(name, GlobalEventExecutor.INSTANCE);
    }

    public void push(Object source) {
        this.channelGroup.writeAndFlush(source);
    }

    public AppInfo add(ChannelHandlerContext context) {
        this.channelGroup.add(context.channel());
        return this;
    }

    public void remove(ChannelHandlerContext context) {
        this.channelGroup.remove(context.channel());
    }

    public int size() {
        return channelGroup.size();
    }

    public String getName() {
        return name;
    }
}
