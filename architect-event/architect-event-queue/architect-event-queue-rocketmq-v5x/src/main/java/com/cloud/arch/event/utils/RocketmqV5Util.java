package com.cloud.arch.event.utils;

import com.cloud.arch.event.props.RocketmqV5Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.springframework.util.Assert;

public class RocketmqV5Util {

    public static final String FILTER_TAG_REGEX        = "*";
    public static final String COMPOSITE_TAG_DELIMITER = "||";

    private static SessionCredentialsProvider getCredentialsProvider(RocketmqV5Properties properties) {
        if (StringUtils.isBlank(properties.getAccessKey()) || StringUtils.isBlank(properties.getSecretKey())) {
            return null;
        }
        if (StringUtils.isNotBlank(properties.getSecurityToken())) {
            return new StaticSessionCredentialsProvider(properties.getAccessKey(), properties.getSecretKey(), properties.getSecurityToken());
        }
        return new StaticSessionCredentialsProvider(properties.getAccessKey(), properties.getSecretKey());
    }

    public static ClientConfiguration createConfiguration(RocketmqV5Properties properties) {
        String endpoints = properties.getEndpoints();
        Assert.state(StringUtils.isNotBlank(endpoints), "rocketmq endpoints must not be null.");
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder().setEndpoints(endpoints)
                                                                .setRequestTimeout(properties.getRequestTimeout());
        SessionCredentialsProvider credentialsProvider = getCredentialsProvider(properties);
        if (credentialsProvider != null) {
            builder.setCredentialProvider(credentialsProvider);
        }
        return builder.build();
    }

}
