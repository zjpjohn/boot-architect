package com.cloud.arch.hotkey.coder;

import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import com.cloud.arch.hotkey.utils.ProtostuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class CommandEncoder extends MessageToByteEncoder<HotkeyCommand> {

    @Override
    protected void encode(ChannelHandlerContext context, HotkeyCommand input, ByteBuf output) throws Exception {
        byte[] bytes     = ProtostuffUtils.serialize(input);
        byte[] delimiter = HotkeyConstants.DELIMITER.getBytes(StandardCharsets.UTF_8);

        byte[] total = new byte[bytes.length + delimiter.length];
        System.arraycopy(bytes, 0, total, 0, bytes.length);
        System.arraycopy(delimiter, 0, total, bytes.length, delimiter.length);
        output.writeBytes(total);

    }
}
