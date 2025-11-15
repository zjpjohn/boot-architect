package com.cloud.arch.rocket.producer.spring;

import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.cloud.arch.rocket.commons.BaseOnsConfiguration;
import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.producer.core.OnsProducerTemplate;
import com.cloud.arch.rocket.producer.core.OnsRecogniseHandler;
import com.cloud.arch.rocket.producer.tx.LocalTransactionCheckerImpl;
import com.cloud.arch.rocket.producer.tx.OnsTransactionInterceptor;
import com.cloud.arch.rocket.serializable.JsonSerialize;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.transaction.TransactionCheckerContainer;
import com.cloud.arch.rocket.transaction.aspect.TxSenderAnnotationPointcutAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;

import static com.cloud.arch.rocket.utils.RocketOnsConstants.*;


@Slf4j
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnProperty(prefix = "com.cloud.rocket.ons.producer", name = "enable", havingValue = "true")
@EnableConfigurationProperties(OnsQueueProperties.class)
@Import(OnsProducerRegistrar.class)
public class ProducerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Serialize.class)
    public JsonSerialize serialize() {
        return new JsonSerialize();
    }

    @Bean(name = ONS_RECOGNISE_BEAN_NAME)
    public OnsRecogniseHandler recogniseHandler() {
        return new OnsRecogniseHandler();
    }

    @Bean
    @ConditionalOnMissingBean(OnsProducerTemplate.class)
    public OnsProducerTemplate onsProducerTemplate(OnsQueueProperties properties, Serialize serialize) {
        return new OnsProducerTemplate(properties, serialize);
    }

    @Bean
    public ProxyProducerProvider proxyProducerProvider(OnsProducerTemplate producerTemplate,
                                                       OnsRecogniseHandler recogniseHandler) {
        return new ProxyProducerProvider(producerTemplate, recogniseHandler);
    }

    @Configuration
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @EnableConfigurationProperties(OnsQueueProperties.class)
    @ConditionalOnProperty(prefix = "com.cloud.rocket.ons.producer", value = "transaction", havingValue = "true")
    public static class TransactionProducerConfiguration extends BaseOnsConfiguration {

        @Bean
        public TransactionCheckerContainer transactionCheckerContainer() {
            return new TransactionCheckerContainer();
        }

        @Bean(name = LOCAL_TRANSACTION_CHECKER_BEAN_NAME)
        public LocalTransactionChecker localTransactionChecker(TransactionCheckerContainer transactionCheckerContainer) {
            return new LocalTransactionCheckerImpl(transactionCheckerContainer);
        }

        @Bean
        public OnsTransactionInterceptor onsTransactionInterceptor(OnsProducerTemplate onsProducerTemplate) {
            return new OnsTransactionInterceptor(onsProducerTemplate);
        }

        @Bean(name = TRANSACTION_SENDER_ADVISOR_BEAN_NAME)
        public TxSenderAnnotationPointcutAdvisor onsTransactionAdvisor(OnsTransactionInterceptor onsTransactionInterceptor) {
            TxSenderAnnotationPointcutAdvisor pointcutAdvisor = new TxSenderAnnotationPointcutAdvisor();
            pointcutAdvisor.setAdvice(onsTransactionInterceptor);
            pointcutAdvisor.setOrder(Ordered.LOWEST_PRECEDENCE);
            return pointcutAdvisor;
        }
    }

}
