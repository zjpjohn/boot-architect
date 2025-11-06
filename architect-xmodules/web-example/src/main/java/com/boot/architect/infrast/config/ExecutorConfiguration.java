package com.boot.architect.infrast.config;

import com.boot.architect.domain.ability.GenderExecutor;
import com.boot.architect.domain.ability.StateExecutor;
import com.boot.architect.infrast.persist.enums.Gender;
import com.boot.architect.infrast.persist.enums.State;
import com.cloud.arch.executor.EnumerableExecutorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ExecutorConfiguration {


    @Bean
    public EnumerableExecutorFactory<Gender, GenderExecutor> genderEnumerableExecutorFactory() {
        return new EnumerableExecutorFactory<>(Gender.class, GenderExecutor.class);
    }

    @Bean
    public EnumerableExecutorFactory<State, StateExecutor> stateEnumerableExecutorFactory() {
        return new EnumerableExecutorFactory<>(State.class, StateExecutor.class);
    }

}
