package com.boot.architect.application.command.impl;

import com.boot.architect.application.assembler.UserAssembler;
import com.boot.architect.application.command.IUserCommandService;
import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.domain.user.model.UserInfoDo;
import com.boot.architect.domain.user.repository.IUserRepository;
import com.cloud.arch.aggregate.AggregateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService implements IUserCommandService {

    private final UserAssembler   assembler;
    private final IUserRepository repository;

    @Override
    @Transactional
    public void createUser(UserCreateCmd cmd) {
        UserInfoDo userInfo = assembler.toDo(cmd);
        userInfo.create();
        repository.save(AggregateFactory.create(userInfo));
    }

}
