package com.boot.architect.adapter;

import com.boot.architect.application.command.IUserCommandService;
import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.application.query.IUserQueryService;
import com.cloud.arch.web.annotation.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserCommandService userCommandService;
    private final IUserQueryService   userQueryService;

    @PostMapping("/")
    public void createUser(@Validated UserCreateCmd cmd) {
        userCommandService.createUser(cmd);
    }

}
