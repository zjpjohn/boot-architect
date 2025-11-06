package com.boot.architect.application.command.dto;

import com.boot.architect.infrast.persist.enums.Gender;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentEditCmd {

    @NotNull(message = "学生标识为空")
    private Long    id;
    private String  name;
    private Integer age;
    private String  clazz;
    private Gender  gender;

}
