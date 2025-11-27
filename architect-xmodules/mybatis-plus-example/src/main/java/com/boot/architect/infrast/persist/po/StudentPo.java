package com.boot.architect.infrast.persist.po;


import com.baomidou.mybatisplus.annotation.*;
import com.boot.architect.infrast.persist.enums.Gender;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student")
public class StudentPo {

    @TableId(type = IdType.AUTO)
    private Long          id;
    private String        name;
    private Integer       age;
    private Integer       classId;
    private Gender        gender;
    @Version
    private Integer       version;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModify;

}
