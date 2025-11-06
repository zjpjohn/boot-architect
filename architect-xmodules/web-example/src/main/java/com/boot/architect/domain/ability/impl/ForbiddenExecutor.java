package com.boot.architect.domain.ability.impl;

import com.boot.architect.domain.ability.StateExecutor;
import com.boot.architect.infrast.persist.enums.State;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ForbiddenExecutor implements StateExecutor {

    @Override
    public State bizIndex() {
        return State.FORBIDDEN;
    }

    @Override
    public void execute() {
        log.info("执行已封禁执行器");
    }
}
