package com.cloud.arch.rocket.producer.core;

import com.cloud.arch.rocket.meta.MsgSendCallback;
import com.cloud.arch.rocket.meta.MsgSendResult;
import com.cloud.arch.rocket.meta.SenderMetadata;
import com.cloud.arch.rocket.meta.SenderModelHandler;
import com.google.common.collect.Sets;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public enum SendModel implements SenderModelHandler<RocketSendHandler> {
    SYNC() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            //消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];

            RocketProducerTemplate producer = handler.getProducerTemplate();
            //消息业务key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            //同步发送普通消息
            producer.syncSend(handler.getTopic(), handler.getFilterTag(), key, metadata.getTimeout(), payload);
            return null;
        }
    },
    SYNC_BATCH() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);

        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            Collection<? extends Serializable> messages
                    = (Collection<? extends Serializable>) args[metadata.getPayload()];
            Integer                batchSize = metadata.getBatchSize();
            RocketProducerTemplate producer  = handler.getProducerTemplate();
            if (messages.size() <= batchSize) {
                producer.syncBatchSend(handler.getTopic(), handler.getFilterTag(), metadata.getTimeout(), messages);
                return null;
            }
            //超过单次发送量进行分段发送
            int segment = messages.size() / batchSize;
            for (int i = 0; i < segment; i++) {
                List<? extends Serializable> segments = messages.stream().skip((long) i * batchSize).limit(batchSize)
                                                                .collect(Collectors.toList());
                producer.syncBatchSend(handler.getTopic(), handler.getFilterTag(), metadata.getTimeout(), segments);
            }
            return null;
        }
    },
    SYNC_ORDER() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
            Assert.notNull(metadata.getShardingKey(), "顺序消息shardingKey不允许为空.");
        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            //消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            //消息业务key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            //顺序hashKey
            String                 shardingKey = (String) args[metadata.getShardingKey()];
            RocketProducerTemplate producer    = handler.getProducerTemplate();
            //发送同步顺序消息
            producer.syncOrderlySend(handler.getTopic(), handler.getFilterTag(), key, shardingKey, metadata.getTimeout(), payload);
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
            Assert.notNull(metadata.getAsync(), "异步消息回调不允许为空.");
        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            //消息内容
            Serializable payload = (Serializable) args[metadata.getPayload()];
            //异步消息回调
            MsgSendCallback callback = (MsgSendCallback) args[metadata.getAsync()];
            //消息业务key
            String                 key      = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            RocketProducerTemplate producer = handler.getProducerTemplate();
            producer.asyncSend(handler.getTopic(), handler.getFilterTag(), key, metadata.getTimeout(), payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    MsgSendResult result = new MsgSendResult();
                    result.setMsgId(sendResult.getMsgId());
                    result.setTopic(sendResult.getMessageQueue().getTopic());
                    callback.onSuccess(result);
                }

                @Override
                public void onException(Throwable e) {
                    callback.onException(e);
                }
            });
            return null;
        }
    },
    ASYNC_ORDER() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
            Assert.notNull(metadata.getShardingKey(), "异步顺序消息shardingKey参数为空");
            Assert.notNull(metadata.getAsync(), "异步顺序消息回调参数为空");
        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata         metadata    = handler.getMetadata();
            Serializable           payload     = (Serializable) args[metadata.getPayload()];
            String                 key         = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            String                 shardingKey = (String) args[metadata.getShardingKey()];
            MsgSendCallback        callback    = (MsgSendCallback) args[metadata.getAsync()];
            RocketProducerTemplate producer    = handler.getProducerTemplate();
            producer.asyncOrderlySend(handler.getTopic(), handler.getFilterTag(), shardingKey, key, metadata.getTimeout(), payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    MsgSendResult result = new MsgSendResult();
                    result.setMsgId(sendResult.getMsgId());
                    result.setTopic(sendResult.getMessageQueue().getTopic());
                    callback.onSuccess(result);
                }

                @Override
                public void onException(Throwable e) {
                    callback.onException(e);
                }
            });
            return null;
        }
    },
    ONEWAY() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata         metadata = handler.getMetadata();
            Serializable           payload  = (Serializable) args[metadata.getPayload()];
            String                 key      = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            RocketProducerTemplate producer = handler.getProducerTemplate();
            producer.sendOneWay(handler.getTopic(), handler.getFilterTag(), key, payload);
            return null;
        }
    },
    ONEWAY_ORDER() {
        /**
         * 校验发送消息元数据信息
         *
         * @param metadata 元数据信息
         */
        @Override
        public void sendCheck(SenderMetadata metadata) {
            super.sendCheck(metadata);
            Assert.notNull(metadata.getShardingKey(), "顺序消息shardingKey不允许为空");
        }

        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata         metadata    = handler.getMetadata();
            Serializable           payload     = (Serializable) args[metadata.getPayload()];
            String                 key         = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            String                 shardingKey = (String) args[metadata.getShardingKey()];
            RocketProducerTemplate producer    = handler.getProducerTemplate();
            producer.sendOrderlyOneWay(handler.getTopic(), handler.getFilterTag(), key, shardingKey, payload);
            return null;
        }
    },
    DELAY() {
        /**
         * @param handler 消息发送处理器
         * @param args    发送消息方法参数
         */
        @Override
        public Object send(RocketSendHandler handler, Object[] args) {
            SenderMetadata metadata = handler.getMetadata();
            Serializable   payload  = (Serializable) args[metadata.getPayload()];
            //消息Key
            String key = metadata.getKey() != null ? (String) args[metadata.getKey()] : null;
            //发送延迟消息
            SenderMetadata.DelayMetadata meta     = metadata.getDelay();
            RocketProducerTemplate       producer = handler.getProducerTemplate();
            Set<Long>                    delays   = Sets.newHashSet();
            if (meta.isCollection()) {
                delays.addAll((Collection<Long>) args[meta.getIndex()]);
            } else {
                delays.add((Long) args[meta.getIndex()]);
            }
            if (!meta.isDeliver()) {
                long     current  = System.currentTimeMillis();
                TimeUnit timeUnit = meta.getTimeUnit();
                delays = delays.stream().map(e -> timeUnit.toMillis(e) + current).collect(Collectors.toSet());
            }
            producer.syncDeliverSend(handler.getTopic(), handler.getFilterTag(), key, metadata.getTimeout(), delays, payload);
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
            Assert.notNull(metadata.getDelay(), "延迟消息延迟时间必须配置.");
        }
    };

    /**
     * 校验发送消息元数据信息
     *
     * @param metadata 元数据信息
     */
    @Override
    public void sendCheck(SenderMetadata metadata) {
        Assert.notNull(metadata.getPayload(), "消息内容参数不允许为空.");
    }

}
