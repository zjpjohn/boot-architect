package com.boot.architect.domain.ability;

import com.boot.architect.infrast.persist.enums.State;
import com.cloud.arch.executor.Executor;
import com.cloud.arch.executor.ExecPoint;

@ExecPoint
public interface StateExecutor extends Executor<State> {

    void execute();

}
