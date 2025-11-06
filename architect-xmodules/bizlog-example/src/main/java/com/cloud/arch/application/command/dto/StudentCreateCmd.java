package com.cloud.arch.application.command.dto;

import com.cloud.arch.annotation.RptField;
import com.cloud.arch.infrast.persist.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentCreateCmd {

    @NotNull(message = "管理员标识为空")
    private Long    adminId;
    @RptField(table = "student", column = "name", message = "学生名称已存在")
    @NotBlank(message = "学生名称为空")
    private String  name;
    @NotNull(message = "年龄为空")
    private Integer age;
    @NotBlank(message = "班级为空")
    private String  clazz;
    @NotNull(message = "性别为空")
    private Gender  gender;

}
