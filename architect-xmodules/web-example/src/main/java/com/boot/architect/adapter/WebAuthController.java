package com.boot.architect.adapter;

import com.boot.architect.application.command.IAuthCommandService;
import com.boot.architect.application.command.dto.AuthLoginCmd;
import com.boot.architect.application.command.vo.AuthLoginResult;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final IAuthCommandService authCommandService;


    @PostMapping("/user")
    public AuthLoginResult userAuth(@Validated AuthLoginCmd command) {
        return authCommandService.userLogin(command);
    }

    @PostMapping("/manager")
    public AuthLoginResult managerAuth(@Validated AuthLoginCmd command) {
        return authCommandService.managerLogin(command);
    }

}
