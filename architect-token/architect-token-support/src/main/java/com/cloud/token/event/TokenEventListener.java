package com.cloud.token.event;

import com.cloud.token.event.model.TokenEvent;

public interface TokenEventListener<E extends TokenEvent> {

    void onHandle(E event);

}
