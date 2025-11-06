package com.boot.architect.domain.ability.impl;

import com.boot.architect.domain.ability.StateExecutor;
import com.boot.architect.infrast.persist.enums.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateExecutor implements StateExecutor {
    @Override
    public State bizIndex() {
        return State.CREATED;
    }

    @Override
    public void execute() {
        log.info("执行已创建执行器");
    }
}
