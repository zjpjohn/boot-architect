package com.cloud.arch.hotkey.coder;

import com.cloud.arch.hotkey.model.HotkeyCommand;
import com.cloud.arch.hotkey.utils.ProtostuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CommandDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> list) throws Exception {
        try {
            byte[] body = new byte[input.readableBytes()];
            input.readBytes(body);
            HotkeyCommand command = ProtostuffUtils.deserialize(body, HotkeyCommand.class);
            list.add(command);
        } catch (Exception e) {
            log.error("消息解码错误:", e);
        }
    }
}
