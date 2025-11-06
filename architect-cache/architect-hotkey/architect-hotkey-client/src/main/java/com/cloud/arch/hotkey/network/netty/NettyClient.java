package com.cloud.arch.hotkey.network.netty;

import com.cloud.arch.hotkey.coder.CommandDecoder;
import com.cloud.arch.hotkey.coder.CommandEncoder;
import com.cloud.arch.hotkey.network.worker.HotKeyWorkerManager;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class NettyClient {

    private final HotKeyWorkerManager workerManager;
    private       Bootstrap           bootstrap;


    public NettyClient(HotKeyWorkerManager workerManager) {
        this.workerManager = workerManager;
        this.initialize();
    }

    private void initialize() {
        NioEventLoopGroup group = new NioEventLoopGroup(2);
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(group)
                      .channel(NioSocketChannel.class)
                      .option(ChannelOption.SO_KEEPALIVE, true)
                      .option(ChannelOption.TCP_NODELAY, true)
                      .handler(new ChannelInitializer<SocketChannel>() {
                          @Override
                          protected void initChannel(SocketChannel channel) throws Exception {
                              ByteBuf delimiter = Unpooled.copiedBuffer(HotkeyConstants.DELIMITER.getBytes(
                                      StandardCharsets.UTF_8));
                              channel.pipeline()
                                     .addLast(new DelimiterBasedFrameDecoder(HotkeyConstants.MAX_LENGTH, delimiter))
                                     .addLast(new CommandDecoder())
                                     .addLast(new CommandEncoder())
                                     .addLast(new IdleStateHandler(0, 0, 30))
                                     .addLast(new NettyClientHandler(workerManager));
                          }
                      });
    }

    /**
     * 连接热key探测worker
     *
     * @param addresses worker列表
     */
    public synchronized void connect(List<String> addresses) {
        for (String address : addresses) {
            if (workerManager.hasConnected(address)) {
                continue;
            }
            String[] hostPort = address.trim().split(":");
            try {
                ChannelFuture channelFuture = bootstrap.connect(hostPort[0], Integer.parseInt(hostPort[1])).sync();
                Channel       channel       = channelFuture.channel();
                workerManager.put(address, channel);
            } catch (Exception error) {
                log.error("[{}]worker机器链接不上...", address);
                workerManager.put(address, null);
            }
        }
        if (!workerManager.hasConnectedWorkers()) {
            log.error("没有可用的worker机器，请联系确认worker集群.");
        }
    }

}
