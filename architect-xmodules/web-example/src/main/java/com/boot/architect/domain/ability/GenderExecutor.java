package com.boot.architect.domain.ability;

import com.boot.architect.infrast.persist.enums.Gender;
import com.cloud.arch.executor.Executor;
import com.cloud.arch.executor.ExecutorPoint;

@ExecutorPoint
public interface GenderExecutor extends Executor<Gender> {

    void execute();

}
