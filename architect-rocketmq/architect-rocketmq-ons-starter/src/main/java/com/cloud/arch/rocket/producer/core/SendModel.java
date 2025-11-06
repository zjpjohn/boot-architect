package com.cloud.arch.rocket.producer.core;

import com.aliyun.openservices.ons.api.OnExceptionContext;
import com.aliyun.openservices.ons.api.SendCallback;
import com.aliyun.openservices.ons.api.SendResult;
import com.cloud.arch.rocket.meta.MsgSendCallback;
import com.cloud.arch.rocket.meta.MsgSendResult;
import com.cloud.arch.rocket.meta.SenderMetadata;
import com.cloud.arch.rocket.meta.SenderModelHandler;
import com.google.common.collect.Sets;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public enum SendModel implements SenderModelHandler<OnsSendHandler> {
    PLAIN() {
        /**
         * @param handler 消息发送处理器
         * @param args 发送消息方法参数
         */
        @Override
        public Object send(OnsSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            // 消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            // 消息Key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            // 发送普通消息
            OnsProducerTemplate producer = handler.getProducer();
            producer.send(handler.getTopic(), handler.getFilterTag(), key, payload);
            return null;
        }
    },
    ASYNC() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
            Assert.notNull(metadata.getAsync(), "异步消息回调必须配置");
        }

        /**
         * @param handler 消息发送处理器
         * @param args 发送消息方法参数
         */
        @Override
        public Object send(OnsSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            // 消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            // 消息回调
            MsgSendCallback callback = (MsgSendCallback) args[metadata.getAsync()];
            // 消息Key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            // 发送异步消息
            OnsProducerTemplate producer = handler.getProducer();
            producer.sendAsync(handler.getTopic(), handler.getFilterTag(), key, payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    MsgSendResult result = new MsgSendResult();
                    result.setMsgId(sendResult.getMessageId());
                    result.setTopic(sendResult.getTopic());
                    callback.onSuccess(result);
                }

                @Override
                public void onException(OnExceptionContext context) {
                    callback.onException(context.getException());
                }
            });
            return null;
        }
    },
    ONEWAY() {
        /**
         * @param handler 消息发送处理器
         * @param args 发送消息方法参数
         */
        @Override
        public Object send(OnsSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            // 消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            // 消息Key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            // 发送普通消息
            OnsProducerTemplate producer = handler.getProducer();
            producer.sendOneway(handler.getTopic(), handler.getFilterTag(), key, payload);
            return null;
        }
    },
    DELAY() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
            Assert.notNull(metadata.getDelay(), "延迟消息延迟时间必须配置");
        }

        /**
         * @param handler 消息发送处理器
         * @param args 发送消息方法参数
         */
        @Override
        public Object send(OnsSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            // 消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            // 消息Key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            // 发送延迟消息
            SenderMetadata.DelayMetadata meta     = metadata.getDelay();
            TimeUnit                     timeUnit = meta.getTimeUnit();
            OnsProducerTemplate          producer = handler.getProducer();
            Set<Long>                    delays   = Sets.newHashSet();
            if (meta.isCollection()) {
                delays.addAll((Collection<Long>) args[meta.getIndex()]);
            } else {
                delays.add((Long) args[meta.getIndex()]);
            }
            if (!meta.isDeliver()) {
                long current = System.currentTimeMillis();
                delays = delays.stream().map(e -> timeUnit.toMillis(e) + current).collect(Collectors.toSet());
            }
            producer.sendDelay(handler.getTopic(), handler.getFilterTag(), key, delays, payload);
            return null;
        }
    },
    ORDERLY() {
        /**
         * @param handler 消息发送处理器
         * @param args 发送消息方法参数
         */
        @Override
        public Object send(OnsSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            // 消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            // 消息Key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            // 顺序消息shardingKey
            String shardingKey = (String) args[metadata.getShardingKey()];
            // 发送消息
            OnsProducerTemplate producer = handler.getProducer();
            producer.sendOrder(handler.getTopic(), handler.getFilterTag(), key, payload, shardingKey);
            return null;
        }

        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
            Assert.notNull(metadata.getShardingKey(), "顺序消息shardingKey必须配置");
        }
    };

    /**
     * 校验发送消息元数据信息
     *
     * @param metadata 元数据信息
     */
    @Override
    public void sendCheck(SenderMetadata metadata) {
        Assert.notNull(metadata.getPayload(), "消息内容参数必须配置");
    }

}
