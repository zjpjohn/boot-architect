package com.boot.architect.infrast.persist.po;

import com.boot.architect.infrast.persist.enums.Gender;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("student")
public class StudentPo {

    @Id
    private Long          id;
    private String        name;
    private Integer       age;
    private Integer       classId;
    private Gender        gender;
    @Column(version = true)
    private Integer       version;
    @Column(onInsertValue = "current_time")
    private LocalDateTime gmtCreate;
    @Column(onInsertValue = "current_time", onUpdateValue = "current_time")
    private LocalDateTime gmtModify;

}
