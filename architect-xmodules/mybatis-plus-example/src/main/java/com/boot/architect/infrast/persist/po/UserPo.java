package com.boot.architect.infrast.persist.po;

import com.baomidou.mybatisplus.annotation.*;
import com.boot.architect.infrast.persist.enums.Gender;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_info")
public class UserPo {

    @TableId(type = IdType.ASSIGN_ID)
    private Long          id;
    private String        name;
    private String        nickname;
    private Gender        gender;
    @Version
    private Integer       version;
    @TableLogic(value = "0", delval = "1")
    private Integer       deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModify;
}
