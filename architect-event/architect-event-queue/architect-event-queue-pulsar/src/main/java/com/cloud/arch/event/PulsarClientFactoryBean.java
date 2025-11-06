package com.cloud.arch.event;

import com.cloud.arch.event.props.PulsarMqProperties;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.TimeUnit;

public class PulsarClientFactoryBean implements FactoryBean<PulsarClient>, DisposableBean {

    private final PulsarMqProperties properties;
    private       PulsarClient       pulsarClient;

    public PulsarClientFactoryBean(PulsarMqProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void destroy() throws Exception {
        if (this.pulsarClient != null) {
            this.pulsarClient.close();
        }
    }

    @Override
    public PulsarClient getObject() throws Exception {
        this.pulsarClient = PulsarClient.builder().serviceUrl(this.properties.getEnpoints())
                                        .ioThreads(this.properties.getIoThreads())
                                        .listenerThreads(this.properties.getListenerThreads())
                                        .enableTcpNoDelay(this.properties.isEnableTcpNoDelay())
                                        .keepAliveInterval(this.properties.getKeepAliveInterval(), TimeUnit.SECONDS)
                                        .connectionTimeout(this.properties.getConnectionTimeout(), TimeUnit.SECONDS)
                                        .operationTimeout(this.properties.getOperationTimeout(), TimeUnit.SECONDS)
                                        .maxBackoffInterval(this.properties.getMaxBackoffInterval(), TimeUnit.SECONDS)
                                        .startingBackoffInterval(this.properties.getMaxBackoffInterval(), TimeUnit.MILLISECONDS)
                                        .build();
        return this.pulsarClient;
    }

    @Override
    public Class<?> getObjectType() {
        return PulsarClient.class;
    }

}
