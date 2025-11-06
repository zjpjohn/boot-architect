package com.boot.architect.domain.ability.impl;

import com.boot.architect.domain.ability.GenderExecutor;
import com.boot.architect.infrast.persist.enums.Gender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FemaleExecutor implements GenderExecutor {

    @Override
    public Gender bizIndex() {
        return Gender.FEMALE;
    }

    @Override
    public void execute() {
        log.info("执行[female]执行器");
    }
}
