package com.cloud.arch.hotkey.core.key;

import com.cloud.arch.hotkey.model.HotKeyModel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReceiveNewKeyEvent {

    private HotKeyModel model;

    public ReceiveNewKeyEvent(HotKeyModel model) {
        this.model = model;
    }
}
