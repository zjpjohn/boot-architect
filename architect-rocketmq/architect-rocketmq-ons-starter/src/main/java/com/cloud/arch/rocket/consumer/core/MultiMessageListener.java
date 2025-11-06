package com.cloud.arch.rocket.consumer.core;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.batch.BatchMessageListener;

import java.util.List;

public class MultiMessageListener implements BatchMessageListener {

    @Override
    public Action consume(List<Message> messages, ConsumeContext context) {
        return null;
    }
}
