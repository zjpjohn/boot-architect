package com.cloud.arch.hotkey.model;

import com.cloud.arch.hotkey.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
public class HotkeyCommand {

    private int                 magicNumber;
    private String      appName;
    private MessageType messageType;
    private String      body;
    private List<HotKeyModel>   hotKeyModels;
    private List<KeyCountModel> keyCountModels;

    public HotkeyCommand(MessageType messageType) {
        this(null, messageType);
    }

    public HotkeyCommand(String appName, MessageType messageType) {
        this.appName     = appName;
        this.messageType = messageType;
    }

}
