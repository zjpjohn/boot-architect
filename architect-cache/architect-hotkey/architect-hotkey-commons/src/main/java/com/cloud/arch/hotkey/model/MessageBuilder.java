package com.cloud.arch.hotkey.model;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.hotkey.utils.HotkeyConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class MessageBuilder {

    public static ByteBuf buildByteBuf(String msg) {
        return Unpooled.copiedBuffer((msg + HotkeyConstants.DELIMITER).getBytes(StandardCharsets.UTF_8));
    }

    public static ByteBuf buildByteBuf(HotkeyCommand command) {
        return Unpooled.copiedBuffer((JSON.toJSONString(command)
                                      + HotkeyConstants.DELIMITER).getBytes(StandardCharsets.UTF_8));
    }

}
