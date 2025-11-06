package com.cloud.arch.web;

import com.cloud.arch.web.support.EnumParameterCustomizer;
import com.cloud.arch.web.support.EnumPropertyCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springdoc.core.utils.Constants.SPRINGDOC_ENABLED;

@Slf4j
@Configuration
@ConditionalOnProperty(name = SPRINGDOC_ENABLED, matchIfMissing = true)
public class SpringDocConfiguration {

    @Bean
    public EnumParameterCustomizer enumParameterCustomizer() {
        return new EnumParameterCustomizer();
    }

    @Bean
    public EnumPropertyCustomizer enumPropertyCustomizer() {
        return new EnumPropertyCustomizer();
    }

}
