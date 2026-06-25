package com.boot.architect.domain.ability;

import com.boot.architect.infrast.persist.enums.Gender;
import com.cloud.arch.executor.Executor;

public interface GenderExecutor extends Executor<Gender> {

    void execute();

}
