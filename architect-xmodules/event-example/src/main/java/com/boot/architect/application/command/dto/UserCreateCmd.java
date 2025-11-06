package com.boot.architect.application.command.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateCmd {

    @NotBlank(message = "用户名为空")
    private String name;
    @NotBlank(message = "手机号为空")
    private String phone;

}
