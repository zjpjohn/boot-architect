package com.boot.architect.infrast.config;

import com.boot.architect.domain.ability.GenderExecutor;
import com.boot.architect.domain.ability.StateExecutor;
import com.boot.architect.infrast.persist.enums.Gender;
import com.boot.architect.infrast.persist.enums.State;
import com.cloud.arch.executor.EnumExecutorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ExecutorConfiguration {


    @Bean
    public EnumExecutorFactory<Gender, GenderExecutor> genderEnumerableExecutorFactory() {
        return new EnumExecutorFactory<>(Gender.class, GenderExecutor.class);
    }

    @Bean
    public EnumExecutorFactory<State, StateExecutor> stateEnumerableExecutorFactory() {
        return new EnumExecutorFactory<>(State.class, StateExecutor.class);
    }

}
