package com.cloud.arch.infrast.persist.po;

import com.baomidou.mybatisplus.annotation.*;
import com.cloud.arch.infrast.persist.enums.Gender;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_info")
public class StudentPo {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private String        name;
    private Integer       age;
    private String        clazz;
    private Gender        gender;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModify;

}
