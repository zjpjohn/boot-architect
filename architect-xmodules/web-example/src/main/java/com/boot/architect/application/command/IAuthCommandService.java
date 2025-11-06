package com.boot.architect.application.command;

import com.boot.architect.application.command.dto.AuthLoginCmd;
import com.boot.architect.application.command.vo.AuthLoginResult;

public interface IAuthCommandService {

    AuthLoginResult userLogin(AuthLoginCmd command);

    AuthLoginResult managerLogin(AuthLoginCmd cmd);

}
