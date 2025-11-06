package com.cloud.arch.rocket.producer.spring;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.producer.core.MessageConverter;
import com.cloud.arch.rocket.producer.core.RocketProducerTemplate;
import com.cloud.arch.rocket.producer.core.RocketRecogniseHandler;
import com.cloud.arch.rocket.producer.core.impl.DefaultMessageConverter;
import com.cloud.arch.rocket.producer.tx.DefaultTransactionListener;
import com.cloud.arch.rocket.producer.tx.TransactionCleanScheduler;
import com.cloud.arch.rocket.producer.tx.TransactionProducerContainer;
import com.cloud.arch.rocket.producer.tx.TransactionSenderInterceptor;
import com.cloud.arch.rocket.serializable.JsonSerialize;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.transaction.TransactionCleanHandler;
import com.cloud.arch.rocket.transaction.aspect.TxSenderAnnotationPointcutAdvisor;
import com.cloud.arch.rocket.transaction.impl.JdbcTransactionChecker;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static com.cloud.arch.rocket.utils.RocketmqConstants.*;


@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "com.cloud.rocket.v4x.producer", name = "enable", havingValue = "true")
@EnableConfigurationProperties(RocketmqProperties.class)
@Import(value = RocketProducerRegistrar.class)
public class ProducerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Serialize.class)
    public JsonSerialize serialize() {
        return new JsonSerialize();
    }

    @Bean
    public MessageConverter messageConverter(Serialize serialize) {
        return new DefaultMessageConverter(serialize);
    }

    @Bean(name = SENDER_RECOGNISE_BEAN_NAME)
    public RocketRecogniseHandler recogniseHandler() {
        return new RocketRecogniseHandler();
    }

    @Bean
    public RocketProducerTemplate producerTemplate(RocketmqProperties properties, MessageConverter messageConverter) {
        return new RocketProducerTemplate(properties, messageConverter);
    }

    @Bean
    public ProxyProducerProvider proxyProducerProvider(RocketProducerTemplate producerTemplate) {
        return new ProxyProducerProvider(producerTemplate);
    }

    @Configuration
    @EnableTransactionManagement
    @EnableConfigurationProperties(RocketmqProperties.class)
    @AutoConfigureAfter(DataSourceAutoConfiguration.class)
    @ConditionalOnProperty(prefix = "com.cloud.rocket.v4x.producer", value = "transaction", havingValue = "true")
    public static class TransactionProducerConfiguration {
        @Bean
        @ConditionalOnMissingBean(DataSourceTransactionManager.class)
        public DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean(name = CHECK_SERVICE_BEAN_NAME)
        @ConditionalOnBean(DataSourceTransactionManager.class)
        public JdbcTransactionChecker transactionChecker(DataSourceTransactionManager transactionManager) {
            return new JdbcTransactionChecker(transactionManager);
        }

        @Bean
        public TransactionCleanHandler transactionGarbageHandler(TransactionChecker transactionChecker) {
            return new TransactionCleanHandler(transactionChecker);
        }

        @Bean
        public TransactionCleanScheduler schedule(TransactionCleanHandler garbageHandler,
                                                  RocketmqProperties properties) {
            return new TransactionCleanScheduler(garbageHandler, properties);
        }

        @Bean
        public DefaultTransactionListener transactionListener(TransactionChecker transactionChecker) {
            return new DefaultTransactionListener(transactionChecker);
        }

        @Bean
        public TransactionProducerContainer producerContainer(TransactionListener transactionListener,
                                                              RocketmqProperties properties,
                                                              TransactionChecker transactionChecker,
                                                              MessageConverter messageConverter) {
            return new TransactionProducerContainer(transactionListener,
                                                    properties,
                                                    transactionChecker,
                                                    messageConverter);
        }

        @Bean(name = TRANSACTION_SENDER_INTERCEPTOR)
        public TransactionSenderInterceptor transactionSenderInterceptor(TransactionProducerContainer producerContainer) {
            return new TransactionSenderInterceptor(producerContainer);
        }

        @Bean(name = ROCKET_TRANSACTION_ADVISOR)
        public TxSenderAnnotationPointcutAdvisor transactionAdvisor(TransactionSenderInterceptor senderInterceptor) {
            TxSenderAnnotationPointcutAdvisor pointcutAdvisor = new TxSenderAnnotationPointcutAdvisor();
            pointcutAdvisor.setAdvice(senderInterceptor);
            pointcutAdvisor.setOrder(Ordered.LOWEST_PRECEDENCE);
            return pointcutAdvisor;
        }
    }

}
