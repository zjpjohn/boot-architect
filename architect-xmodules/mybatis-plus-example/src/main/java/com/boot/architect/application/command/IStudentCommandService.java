package com.boot.architect.application.command;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.page.Pager;

public interface IStudentCommandService {

    void create(StudentCreateCmd command);

    StudentPo getStudent(String name);

    StudentPo getStudentById(Long id);

    Page<StudentPo> getStudents(PageQuery query);

    Pager<StudentPo> getStudentList(PageQuery query);

}
