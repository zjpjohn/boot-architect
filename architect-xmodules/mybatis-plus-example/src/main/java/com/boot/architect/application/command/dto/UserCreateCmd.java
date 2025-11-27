package com.boot.architect.application.command.dto;

import com.boot.architect.infrast.persist.enums.Gender;
import com.cloud.arch.annotation.RptField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateCmd {

    @NotBlank(message = "用户名称为空")
    @RptField(table = "user_info", column = "name", message = "学生名称已存在")
    private String name;
    @NotBlank(message = "用户昵称为空")
    private String nickname;
    @NotNull(message = "用户性别为空")
    private Gender gender;

}
