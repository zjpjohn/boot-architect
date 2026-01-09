package com.boot.architect.infrast.persist.po;

import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.infrast.persist.enums.Gender;
import com.cloud.arch.mybatis.core.handler.JsonTypeHandler;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Table("student")
@NoArgsConstructor
public class StudentPo {

    @Id
    private Long          id;
    private String        name;
    private Integer       age;
    private Integer       classId;
    private Gender        gender;
    @Column(typeHandler = JsonTypeHandler.class)
    private List<String>  images;
    @Column(version = true, onInsertValue = "1")
    private Integer       version;
    @Column(onInsertValue = "current_time")
    private LocalDateTime gmtCreate;
    @Column(onInsertValue = "current_time", onUpdateValue = "current_time")
    private LocalDateTime gmtModify;

    public StudentPo(StudentCreateCmd cmd) {
        this.name = cmd.getName();
        this.age = cmd.getAge();
        this.classId = cmd.getClassId();
        this.gender = cmd.getGender();
        this.images = cmd.getImages();
    }
}
