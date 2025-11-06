package com.cloud.arch.rocket.producer.spring;

import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.cloud.arch.rocket.commons.BaseOnsConfiguration;
import com.cloud.arch.rocket.commons.OnsQueueProperties;
import com.cloud.arch.rocket.producer.core.OnsProducerTemplate;
import com.cloud.arch.rocket.producer.core.OnsRecogniseHandler;
import com.cloud.arch.rocket.producer.tx.LocalTransactionCheckerImpl;
import com.cloud.arch.rocket.producer.tx.OnsTransactionInterceptor;
import com.cloud.arch.rocket.producer.tx.TransactionCleanScheduler;
import com.cloud.arch.rocket.serializable.JsonSerialize;
import com.cloud.arch.rocket.serializable.Serialize;
import com.cloud.arch.rocket.transaction.TransactionChecker;
import com.cloud.arch.rocket.transaction.TransactionCleanHandler;
import com.cloud.arch.rocket.transaction.aspect.TxSenderAnnotationPointcutAdvisor;
import com.cloud.arch.rocket.transaction.impl.JdbcTransactionChecker;
import lombok.extern.slf4j.Slf4j;
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
    @EnableTransactionManagement
    @EnableConfigurationProperties(OnsQueueProperties.class)
    @AutoConfigureAfter(DataSourceAutoConfiguration.class)
    @ConditionalOnProperty(prefix = "com.cloud.rocket.ons.producer", value = "transaction", havingValue = "true")
    public static class TransactionProducerConfiguration extends BaseOnsConfiguration {

        @Bean
        @ConditionalOnMissingBean(DataSourceTransactionManager.class)
        public DataSourceTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean(name = CHECK_SERVICE_BEAN_NAME)
        @ConditionalOnBean(DataSourceTransactionManager.class)
        public TransactionChecker transactionChecker(DataSourceTransactionManager transactionManager) {
            return new JdbcTransactionChecker(transactionManager);
        }

        @Bean
        public TransactionCleanHandler transactionGarbageHandler(TransactionChecker transactionChecker) {
            return new TransactionCleanHandler(transactionChecker);
        }

        @Bean
        public TransactionCleanScheduler schedule(TransactionCleanHandler garbageHandler,
                                                  OnsQueueProperties queueProperties) {
            return new TransactionCleanScheduler(garbageHandler, queueProperties);
        }

        @Bean(name = LOCAL_TRANSACTION_CHECKER_BEAN_NAME)
        public LocalTransactionChecker localTransactionChecker(TransactionChecker transactionChecker) {
            return new LocalTransactionCheckerImpl(transactionChecker);
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
