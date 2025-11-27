package com.boot.architect.application.command.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserEditCmd {

    @NotNull(message = "用户标识为空")
    private Long   id;
    private String name;
    private String nickname;

}
