package com.cloud.arch.application.command;

import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.page.Pager;

public interface IStudentCommandService {

    StudentPo createStudent(StudentCreateCmd cmd);

    StudentPo getStudent(String name);

    StudentPo getStudent(Long id);

    Pager<StudentPo> getStudentList(PageQuery query);

    Pager<StudentPo> getStudents(PageQuery query);

}
