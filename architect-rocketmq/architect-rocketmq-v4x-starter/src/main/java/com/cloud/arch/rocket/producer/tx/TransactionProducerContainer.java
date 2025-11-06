package com.cloud.arch.rocket.producer.tx;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.producer.core.MessageConverter;
import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.utils.RocketmqUtils;
import com.cloud.arch.utils.IdWorker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TransactionProducerContainer implements SmartInitializingSingleton, DisposableBean {

    private final TransactionListener   transactionListener;
    private final RocketmqProperties    properties;
    private final TransactionChecker    transactionChecker;
    private final MessageConverter      converter;
    private       TransactionMQProducer producer;

    public TransactionProducerContainer(TransactionListener transactionListener,
                                        RocketmqProperties properties,
                                        TransactionChecker transactionChecker,
                                        MessageConverter converter) {
        this.transactionListener = transactionListener;
        this.properties          = properties;
        this.transactionChecker  = transactionChecker;
        this.converter           = converter;
    }

    /**
     * 发送事务消息
     *
     * @param topic   消息主题
     * @param tag     消息tag
     * @param key     消息业务key
     * @param payload 事物消息内容
     * @param extra   事务消息附加参数
     */
    public <T extends Serializable> SendResult sendTransaction(String topic,
                                                               String tag,
                                                               String key,
                                                               T payload,
                                                               Object extra) throws Exception {
        Assert.notNull(producer.getTransactionListener(), "transaction producer does not exist transaction listener.");
        Message message = converter.convert(topic, tag, key, payload, null);
        message.setTransactionId(String.valueOf(IdWorker.nextId()));
        return producer.sendMessageInTransaction(message, extra);
    }

    /**
     * 初始化事物消息生产者
     */
    private void initializeProducer() throws Exception {
        String                                        nameSrv     = properties.getNameSrv();
        RocketmqProperties.RocketmqProducerProperties producerCfg = properties.getProducer();
        this.producer
                = RocketmqUtils.creatTransactionProducer(producerCfg.getGroup(), properties.getAccessKey(), properties.getSecretKey(), producerCfg.isEnableTrace(), producerCfg.getTraceTopic());
        this.producer.setNamesrvAddr(nameSrv);
        String accessChannel = properties.getAccessChannel();
        if (StringUtils.isNotBlank(accessChannel)) {
            this.producer.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }
        this.producer.setSendMsgTimeout(producerCfg.getSendMessageTimeout());
        this.producer.setRetryTimesWhenSendFailed(producerCfg.getRetryTimesWhenSendFailed());
        this.producer.setRetryTimesWhenSendAsyncFailed(producerCfg.getRetryTimesWhenSendAsyncFailed());
        this.producer.setMaxMessageSize(producerCfg.getMaxMessageSize());
        this.producer.setCompressMsgBodyOverHowmuch(producerCfg.getCompressMsgBodyThrottle());
        this.producer.setRetryAnotherBrokerWhenNotStoreOK(producerCfg.isRetryNextServer());
        //设置事物消息业务处理以及事务回查监听器
        this.producer.setTransactionListener(transactionListener);
        ExecutorService checkExecutor
                = new ThreadPoolExecutor(producerCfg.getCheckThreadPoolMinSize(), producerCfg.getCheckThreadPoolMaxSize(), producerCfg.getKeepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(producerCfg.getCheckRequestHoldMax()));
        this.producer.setExecutorService(checkExecutor);
        this.producer.start();
    }

    @Override
    @SneakyThrows
    public void afterSingletonsInstantiated() {
        initializeProducer();
    }

    @Override
    public void destroy() throws Exception {
        Optional.ofNullable(this.producer).ifPresent(TransactionMQProducer::shutdown);
    }


    public TransactionListener getTransactionListener() {
        return transactionListener;
    }

    public RocketmqProperties getProperties() {
        return properties;
    }

    public TransactionChecker getTransactionChecker() {
        return transactionChecker;
    }

    public TransactionMQProducer getProducer() {
        return producer;
    }
}
