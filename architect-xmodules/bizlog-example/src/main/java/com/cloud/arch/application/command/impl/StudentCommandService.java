package com.cloud.arch.application.command.impl;

import com.cloud.arch.annotation.RptCheck;
import com.cloud.arch.application.assembler.StudentAssembler;
import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.mapper.StudentMapper;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.mybatis.extend.LambdaQuery;
import com.cloud.arch.mybatis.extend.Query;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.page.Pager;
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
    public StudentPo getStudent(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public StudentPo getStudent(String name) {
        return LambdaQuery.of(mapper).eq(StudentPo::getName, name).one();
    }

    @Override
    public Pager<StudentPo> getStudentList(PageQuery query) {
        return Query.of(mapper).orderByDesc("gmtCreate").pager(query.getPage(), query.getLimit());
    }

    @Override
    public Pager<StudentPo> getStudents(PageQuery query) {
        return LambdaQuery.of(mapper).orderByAsc(StudentPo::getGmtCreate).pager(query.getPage(), query.getLimit());
    }

}
