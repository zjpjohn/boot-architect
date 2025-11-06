package com.cloud.arch.hotkey.net.admin;

import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.model.MessageBuilder;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdminClient {

    private final Bootstrap bootstrap;

    public AdminClient() {
        this.bootstrap = this.initialize();
    }

    private Bootstrap initialize() {
        final NioEventLoopGroup group     = new NioEventLoopGroup(2);
        Bootstrap               bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
                 .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<Channel>() {

                     @Override
                     protected void initChannel(Channel channel) throws Exception {
                         final ByteBuf delimiter = Unpooled.copiedBuffer(HotkeyConstants.DELIMITER.getBytes());
                         channel.pipeline().addLast(new DelimiterBasedFrameDecoder(HotkeyConstants.MAX_LENGTH, delimiter))
                                .addLast(new StringDecoder()).addLast(new IdleStateHandler(0, 0, 30))
                                .addLast(new AdminClientHandler());

                     }
                 });
        return bootstrap;
    }

    public synchronized void connect(String address) {
        if (AdminClientHolder.isConnected()) {
            return;
        }
        final String[] split = address.split(":");
        try {
            final ChannelFuture channelFuture = bootstrap.connect(split[0], Integer.parseInt(split[1])).sync();
            AdminClientHolder.connect(channelFuture.channel());
        } catch (InterruptedException error) {
            AdminClientHolder.disConnect();
            log.error("connect admin server error.", error);
        }
    }

    public synchronized void disConnect() {
        AdminClientHolder.disConnect();
    }

    private static class AdminClientHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            AdminClientHolder.disConnect();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                final IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.ALL_IDLE) {
                    final HotkeyCommand command = new HotkeyCommand(MessageType.PING);
                    ctx.writeAndFlush(MessageBuilder.buildByteBuf(command));
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        }
    }
}
