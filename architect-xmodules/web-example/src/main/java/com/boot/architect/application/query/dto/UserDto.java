package com.boot.architect.application.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserDto {

    @Schema(name = "用户名")
    private String name;
    @Schema(name = "班级")
    private String clazz;
    @Schema(name = "性别")
    private Gender gender;

}
