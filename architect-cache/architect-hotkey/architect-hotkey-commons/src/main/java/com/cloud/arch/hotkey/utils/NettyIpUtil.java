package com.cloud.arch.hotkey.utils;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NettyIpUtil {

    public static final String EMPTY_IP = "";

    /**
     * 获取netty连接中的ip地址
     *
     * @param ctx 连接通道信息
     */
    public static String clientIp(ChannelHandlerContext ctx) {
        try {
            InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
            return socket.getAddress().getHostAddress();
        } catch (Exception e) {
            log.error("获取netty连接IP地址错误:{}", e.getMessage());
        }
        return EMPTY_IP;
    }
}
