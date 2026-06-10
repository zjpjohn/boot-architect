package com.boot.architect.infrast.persist.po;

import com.boot.architect.infrast.persist.enums.Gender;
import com.cloud.arch.web.mask.Mask;
import com.cloud.arch.web.mask.MaskType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("user_info")
public class UserPo {

    @Id
    private Long          id;
    @Mask(type = MaskType.NAME)
    private String        name;
    private String        nickname;
    private Gender        gender;
    @Column(version = true, onInsertValue = "1")
    private Integer       version;
    @Column(isLogicDelete = true)
    private Integer       deleted;
    @Column(onInsertValue = "current_time")
    private LocalDateTime gmtCreate;
    @Column(onInsertValue = "current_time", onUpdateValue = "current_time")
    private LocalDateTime gmtModify;

}
