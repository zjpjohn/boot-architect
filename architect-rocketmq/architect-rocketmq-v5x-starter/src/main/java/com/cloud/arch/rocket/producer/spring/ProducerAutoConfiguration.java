package com.cloud.arch.rocket.producer.spring;

import com.cloud.arch.rocket.commons.RocketmqProperties;
import com.cloud.arch.rocket.producer.core.MessageConverter;
import com.cloud.arch.rocket.producer.core.RocketProducerTemplate;
import com.cloud.arch.rocket.producer.core.RocketRecogniseHandler;
import com.cloud.arch.rocket.producer.core.impl.DefaultMessageConverter;
import com.cloud.arch.rocket.producer.tx.DefaultTransactionListener;
import com.cloud.arch.rocket.producer.tx.TransactionProducerContainer;
import com.cloud.arch.rocket.producer.tx.TransactionSenderInterceptor;
import com.cloud.arch.rocket.serializable.JsonSerialize;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.transaction.TransactionCheckerContainer;
import com.cloud.arch.rocket.transaction.aspect.TxSenderAnnotationPointcutAdvisor;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static com.cloud.arch.rocket.utils.RocketmqUtils.*;


@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "com.cloud.rocket.v5x.producer", name = "enable", havingValue = "true")
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
    @ConditionalOnProperty(prefix = "com.cloud.rocket.v5x.producer", value = "transaction", havingValue = "true")
    public static class TransactionProducerConfiguration {

        @Bean
        public TransactionCheckerContainer transactionCheckerContainer() {
            return new TransactionCheckerContainer();
        }

        @Bean
        public DefaultTransactionListener transactionListener(TransactionCheckerContainer transactionCheckerContainer) {
            return new DefaultTransactionListener(transactionCheckerContainer);
        }

        @Bean
        public TransactionProducerContainer producerContainer(TransactionListener transactionListener,
                                                              RocketmqProperties properties,
                                                              MessageConverter messageConverter) {
            return new TransactionProducerContainer(transactionListener, properties, messageConverter);
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
