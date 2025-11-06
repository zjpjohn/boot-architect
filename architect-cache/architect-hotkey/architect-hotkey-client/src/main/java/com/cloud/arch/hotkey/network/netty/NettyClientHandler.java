package com.cloud.arch.hotkey.network.netty;

import com.cloud.arch.hotkey.core.key.ReceiveNewKeyEvent;
import com.cloud.arch.hotkey.detector.EventBusCenter;
import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotKeyModel;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.network.worker.HotKeyWorkerManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<HotkeyCommand> {

    private final HotKeyWorkerManager workerManager;

    public NettyClientHandler(HotKeyWorkerManager workerManager) {
        this.workerManager = workerManager;
    }

    /**
     * 连接成功，注册本应用APP
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new HotkeyCommand(this.workerManager.getAppName(), MessageType.APP_NAME));
    }

    /**
     * 当前连接断开，清除本地缓存机器列表
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel           channel       = ctx.channel();
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        String            address       = socketAddress.getHostName() + ":" + socketAddress.getPort();
        workerManager.removeInactiveWorker(address);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if ((evt instanceof IdleStateEvent event) && event.state() == IdleState.ALL_IDLE) {
            ctx.writeAndFlush(new HotkeyCommand(this.workerManager.getAppName(), MessageType.PING));
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HotkeyCommand command) throws Exception {
        //热key探测响应
        if (MessageType.RESPONSE_NEW_KEY == command.getMessageType()) {
            List<HotKeyModel> models = command.getHotKeyModels();
            if (CollectionUtils.isEmpty(models)) {
                return;
            }
            for (HotKeyModel model : models) {
                EventBusCenter.post(new ReceiveNewKeyEvent(model));
            }
        }
    }
}
