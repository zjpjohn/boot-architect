package com.boot.architect.application.command;

import com.boot.architect.application.command.dto.UserCreateCmd;

public interface IUserCommandService {

    void createUser(UserCreateCmd cmd);

}
