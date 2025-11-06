package com.cloud.arch.hotkey.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FlushUtil {

    /**
     * 向channel输出消息
     */
    public static void flush(ChannelHandlerContext context, ByteBuf byteBuf) {
        Channel channel = context.channel();
        if (channel.isWritable()) {
            channel.writeAndFlush(byteBuf).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("async flush error:{}", future.cause().getMessage());
                }
            });
            return;
        }
        try {
            channel.writeAndFlush(byteBuf).sync();
        } catch (InterruptedException e) {
            log.error("sync flush error:{}", e.getMessage());
        }
    }
}
