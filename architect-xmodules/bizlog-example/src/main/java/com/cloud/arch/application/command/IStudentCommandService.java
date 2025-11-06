package com.cloud.arch.application.command;

import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.po.StudentPo;

public interface IStudentCommandService {

    StudentPo createStudent(StudentCreateCmd cmd);

}
