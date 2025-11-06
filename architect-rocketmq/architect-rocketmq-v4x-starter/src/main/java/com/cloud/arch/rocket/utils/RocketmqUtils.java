package com.cloud.arch.rocket.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.remoting.RPCHook;

@Slf4j
public class RocketmqUtils {

    public static DefaultMQProducer createProducer(String group, String ak, String sk, boolean enableTrace, String traceTopic) {
        boolean enableAcl = StringUtils.isNotBlank(ak) && StringUtils.isNotBlank(sk);
        RPCHook rpcHook   = null;
        if (enableAcl) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(ak, sk));
        }
        DefaultMQProducer producer = new DefaultMQProducer(group, rpcHook, enableTrace, traceTopic);
        producer.setSendMessageWithVIPChannel(enableAcl);
        return producer;
    }

    public static TransactionMQProducer creatTransactionProducer(String group, String ak, String sk, boolean enableTrace, String traceTopic) {
        boolean enableAcl = StringUtils.isNotBlank(ak) && StringUtils.isNotBlank(sk);
        RPCHook rpcHook   = null;
        if (enableAcl) {
            rpcHook = new AclClientRPCHook(new SessionCredentials(ak, sk));
        }
        TransactionMQProducer producer = new TransactionMQProducer(null, group, rpcHook, enableTrace, traceTopic);
        producer.setVipChannelEnabled(enableAcl);
        return producer;
    }

    public static String getInstanceName(String identify) {
        char separator = '@';
        return identify +
                separator + UtilAll.getPid();
    }

}
