package com.cloud.arch.rocket.producer.tx;

import com.aliyun.openservices.ons.api.Message;

public interface TransactionBusinessHandler {

    void handle(Message message);
}
