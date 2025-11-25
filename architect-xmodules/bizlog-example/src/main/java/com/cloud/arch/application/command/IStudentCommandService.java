package com.cloud.arch.application.command;

import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.mybatis.extend.Pager;
import com.cloud.arch.page.PageQuery;

public interface IStudentCommandService {

    StudentPo createStudent(StudentCreateCmd cmd);

    StudentPo getStudent(String name);

    Pager<StudentPo> getStudentList(PageQuery query);

}
