package com.boot.architect.domain.ability;

import com.boot.architect.infrast.persist.enums.State;
import com.cloud.arch.executor.Executor;

public interface StateExecutor extends Executor<State> {

    void execute();

}
