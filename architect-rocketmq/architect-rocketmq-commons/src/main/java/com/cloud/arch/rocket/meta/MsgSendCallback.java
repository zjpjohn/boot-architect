package com.cloud.arch.rocket.meta;

public interface MsgSendCallback {

    void onSuccess(MsgSendResult sendResult);

    void onException(Throwable error);
}
