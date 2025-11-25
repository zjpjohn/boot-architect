package com.cloud.arch.application.command.impl;

import com.cloud.arch.annotation.RptCheck;
import com.cloud.arch.application.assembler.StudentAssembler;
import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.mapper.StudentMapper;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.mybatis.extend.LambdaQuery;
import com.cloud.arch.mybatis.extend.Pager;
import com.cloud.arch.page.PageQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCommandService implements IStudentCommandService {

    private final StudentMapper    mapper;
    private final StudentAssembler assembler;

    @Override
    @RptCheck
    public StudentPo createStudent(StudentCreateCmd cmd) {
        StudentPo student = assembler.toPo(cmd);
        mapper.insert(student);
        return student;
    }

    @Override
    public StudentPo getStudent(String name) {
        return LambdaQuery.from(StudentPo.class).eq(StudentPo::getName, name).one(mapper::selectOne);
    }

    @Override
    public Pager<StudentPo> getStudentList(PageQuery query) {
        return LambdaQuery.from(StudentPo.class)
                          .orderByDesc(StudentPo::getGmtCreate)
                          .page(query.getPage(), query.getLimit())
                          .pageList(mapper::selectPage);
    }

}
