package com.boot.architect.application.command.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.architect.application.assembler.StudentAssembler;
import com.boot.architect.application.command.IStudentCommandService;
import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.infrast.persist.mapper.StudentMapper;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.annotation.RptCheck;
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
    public void create(StudentCreateCmd command) {
        StudentPo student = assembler.toPo(command);
        mapper.insert(student);
    }

    @Override
    public StudentPo getStudentById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public StudentPo getStudent(String name) {
        return Query.of(mapper).eq("name", name).one();
    }

    @Override
    public Page<StudentPo> getStudents(PageQuery query) {
        return LambdaQuery.of(mapper).orderByDesc(StudentPo::getGmtCreate).page(query.getPage(), query.getLimit());
    }

    @Override
    public Pager<StudentPo> getStudentList(PageQuery query) {
        return LambdaQuery.of(mapper).orderByDesc(StudentPo::getGmtCreate).pager(query.getPage(), query.getLimit());
    }

}
