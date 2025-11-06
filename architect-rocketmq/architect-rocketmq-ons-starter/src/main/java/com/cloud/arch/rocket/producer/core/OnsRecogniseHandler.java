package com.cloud.arch.rocket.producer.core;


import com.cloud.arch.rocket.annotations.Sender;
import com.cloud.arch.rocket.meta.SenderRecogniseHandler;

public class OnsRecogniseHandler implements SenderRecogniseHandler<SendModel> {

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
        if (sender.orderly()) {
            return SendModel.ORDERLY;
        }
        if (sender.async()) {
            return SendModel.ASYNC;
        }
        if (sender.oneWay()) {
            return SendModel.ONEWAY;
        }
        return SendModel.PLAIN;
    }
}
