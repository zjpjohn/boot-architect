package com.cloud.arch.hotkey.net.server;

import com.cloud.arch.hotkey.coder.CommandDecoder;
import com.cloud.arch.hotkey.coder.CommandEncoder;
import com.cloud.arch.hotkey.config.WorkerServerScheduler;
import com.cloud.arch.hotkey.config.props.WorkerNetProperties;
import com.cloud.arch.hotkey.key.KeyRuleManager;
import com.cloud.arch.hotkey.net.admin.AdminClientWatcher;
import com.cloud.arch.hotkey.utils.CpuNum;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import com.google.inject.Inject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class HotkeyWorkerServer implements Closeable {

    private final CountDownLatch    shutdownLatch = new CountDownLatch(1);
    private final NioEventLoopGroup bossGroup     = new NioEventLoopGroup(1);
    private final NioEventLoopGroup workerGroup   = new NioEventLoopGroup(CpuNum.workerCount());
    private final AtomicBoolean     hasShutdown   = new AtomicBoolean(false);

    private final WorkerNetProperties   properties;
    private final HotkeyWorkerHandler   hotkeyWorkerHandler;
    private final WorkerServerScheduler serverScheduler;
    private final KeyRuleManager        keyRuleManager;
    private final WorkerRegister        workerRegister;
    private final WorkerKeyReporter     workerKeyReporter;
    private final AdminClientWatcher    adminClientWatcher;

    @Inject
    public HotkeyWorkerServer(WorkerNetProperties properties,
                              WorkerServerScheduler serverScheduler,
                              KeyRuleManager keyRuleManager,
                              WorkerRegister workerRegister,
                              WorkerKeyReporter workerKeyReporter,
                              AdminClientWatcher adminClientWatcher,
                              HotkeyWorkerHandler hotkeyWorkerHandler) {
        this.properties          = properties;
        this.serverScheduler     = serverScheduler;
        this.keyRuleManager      = keyRuleManager;
        this.workerRegister      = workerRegister;
        this.workerKeyReporter   = workerKeyReporter;
        this.adminClientWatcher  = adminClientWatcher;
        this.hotkeyWorkerHandler = hotkeyWorkerHandler;
    }

    public void startServer() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                     .handler(new LoggingHandler(LogLevel.INFO)).option(ChannelOption.SO_BACKLOG, 1024)
                     .childOption(ChannelOption.SO_KEEPALIVE, true)
                     .childHandler(new ChildChannelHandler(hotkeyWorkerHandler));
            //启动服务器
            ChannelFuture channelFuture = bootstrap.bind(properties.getPort()).sync();
            //定时上报worker机器信息
            workerRegister.scheduleReport();
            //连接admin服务器
            adminClientWatcher.connectAndSchedule();
            //定时拉取热key规则
            keyRuleManager.schedulePullRules();
            //监听规则表换
            keyRuleManager.watchRuleChange();
            //定时上报热key计算状态
            workerKeyReporter.scheduleUploadClient();
            //阻塞监听端口
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("fatal error during hotkey worker start.", e);
            try {
                this.close();
            } catch (IOException ex) {
                log.error("error occurred during server closing.", e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (hasShutdown.compareAndSet(false, true)) {
            bossGroup.shutdownGracefully(1000, 3000, TimeUnit.MILLISECONDS);
            workerGroup.shutdownGracefully(1000, 3000, TimeUnit.MILLISECONDS);
            workerRegister.detachWorker();
            serverScheduler.destroy();
            shutdownLatch.countDown();
        }
    }

    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    public static class ChildChannelHandler extends ChannelInitializer<Channel> {

        private final HotkeyWorkerHandler hotkeyWorkerHandler;

        public ChildChannelHandler(HotkeyWorkerHandler hotkeyWorkerHandler) {
            this.hotkeyWorkerHandler = hotkeyWorkerHandler;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            ByteBuf delimiter = Unpooled.copiedBuffer(HotkeyConstants.DELIMITER.getBytes(StandardCharsets.UTF_8));
            channel.pipeline().addLast(new DelimiterBasedFrameDecoder(HotkeyConstants.MAX_LENGTH, delimiter))
                   .addLast(new CommandDecoder()).addLast(new CommandEncoder()).addLast(hotkeyWorkerHandler);
        }
    }
}
