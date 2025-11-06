package com.cloud.arch.event.extension.storage;


import com.cloud.arch.event.*;
import com.cloud.arch.event.remoting.HttpRemoting;
import com.cloud.arch.event.remoting.RemotingResponseHandler;
import com.cloud.arch.event.rocksdb.RocksdbStorage;
import com.cloud.arch.event.storage.IDomainEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;

@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnClass(name = "com.cloud.arch.event.RocksDomainEventRepository")
@ConditionalOnProperty(prefix = "com.cloud.event.publisher", name = "enable")
public class RocksdbStorageExtensionConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "com.cloud.event.publisher.rocksdb")
    public RocksCompensateProperties compensateProperties() {
        return new RocksCompensateProperties();
    }

    @Bean
    public RocksdbStorage rocksdbStorage(RocksCompensateProperties properties) {
        return new RocksdbStorage(properties.getEventPath());
    }

    @Bean
    public RemotingResponseHandler responseHandler(RocksdbStorage storage) {
        return new RocksResponseHandler(storage);
    }

    @Bean
    public HttpRemoting httpRemoting(RocksCompensateProperties properties, RemotingResponseHandler responseHandler) {
        return new HttpRemoting(properties, responseHandler);
    }

    @Bean
    public RocksReparationProcessor reparationProcessor(HttpRemoting remoting, RocksCompensateProperties properties) {
        return new RocksReparationProcessor(remoting, properties);
    }

    @Bean
    @Primary
    public IDomainEventRepository domainEventRepository(RocksdbStorage storage, RocksReparationProcessor processor) {
        return new RocksDomainEventRepository(storage, processor);
    }

    @Bean
    public RocksFailReparationScheduler reparationScheduler(IDomainEventRepository eventRepository,
                                                            RocksReparationProcessor processor,
                                                            RocksCompensateProperties properties) {
        return new RocksFailReparationScheduler(eventRepository, processor, properties);
    }

}
