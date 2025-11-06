package com.cloud.arch.hotkey.net.admin;


import com.cloud.arch.hotkey.enums.MessageType;
import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.model.MessageBuilder;
import io.netty.channel.Channel;

public class AdminClientHolder {

    private static boolean connected = false;
    private static Channel channel   = null;

    public static void connect(Channel ch) {
        connected = true;
        channel   = ch;
    }

    public static void disConnect() {
        connected = false;
        channel   = null;
    }

    public static boolean isConnected() {
        return connected;
    }

    public static Channel getChannel() {
        return channel;
    }

    public static void push(String command) {
        final HotkeyCommand hotkeyCommand = new HotkeyCommand(MessageType.REQUEST_HOT_KEY);
        hotkeyCommand.setBody(command);
        channel.writeAndFlush(MessageBuilder.buildByteBuf(hotkeyCommand));
    }

}
