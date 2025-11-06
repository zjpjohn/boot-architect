package com.boot.architect.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginCmd {

    @NotBlank(message = "账户名为空")
    private String name;
    @NotBlank(message = "账户密码为空")
    private String password;

}
