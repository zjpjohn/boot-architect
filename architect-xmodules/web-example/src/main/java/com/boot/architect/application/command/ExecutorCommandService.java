package com.boot.architect.application.command;

import com.boot.architect.domain.ability.GenderExecutor;
import com.boot.architect.domain.ability.StateExecutor;
import com.boot.architect.infrast.persist.enums.Gender;
import com.boot.architect.infrast.persist.enums.State;
import com.cloud.arch.executor.ExecutorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutorCommandService {

    private final ExecutorFactory executorFactory;

    public void execute(Gender gender) {
        executorFactory.<Gender, GenderExecutor>of(gender).execute();
    }

    public void execute(State state) {
        executorFactory.<State, StateExecutor>of(state).execute();
    }

}
