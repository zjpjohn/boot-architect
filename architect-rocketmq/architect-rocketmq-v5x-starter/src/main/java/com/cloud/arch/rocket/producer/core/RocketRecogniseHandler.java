package com.cloud.arch.rocket.producer.core;


import com.cloud.arch.rocket.annotations.Sender;
import com.cloud.arch.rocket.meta.SenderRecogniseHandler;

public class RocketRecogniseHandler implements SenderRecogniseHandler<SendModel> {

    /**
     * 根据@Sender识别不同的消息发送模式
     *
     * @param sender 消息发送者注解
     */
    @Override
    public SendModel recognise(Sender sender) {
        if (sender.delay()) {
            return SendModel.DELAY;
        }
        if (sender.oneWay()) {
            return sender.orderly() ? SendModel.ONEWAY_ORDER : SendModel.ONEWAY;
        }
        if (sender.async()) {
            return sender.orderly() ? SendModel.ASYNC_ORDER : SendModel.ASYNC;
        }
        if (sender.batch()) {
            return SendModel.SYNC_BATCH;
        }
        if (sender.orderly()) {
            return SendModel.SYNC_ORDER;
        }
        return SendModel.SYNC;
    }
}
