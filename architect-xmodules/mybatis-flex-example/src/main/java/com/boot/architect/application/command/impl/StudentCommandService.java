package com.boot.architect.application.command.impl;

import com.boot.architect.application.assembler.StudentAssembler;
import com.boot.architect.application.command.IStudentCommandService;
import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.infrast.persist.mapper.StudentMapper;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.mybatis.extend.Query;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.page.Pager;
import com.mybatisflex.core.paginate.Page;
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
    public void create(StudentCreateCmd command) {
        StudentPo student = assembler.toPo(command);
        mapper.insert(student, true);
    }

    @Override
    public StudentPo getStudentById(Long id) {
        return mapper.selectOneById(id);
    }

    @Override
    public StudentPo getStudent(String name) {
        return Query.of(mapper).eq(StudentPo::getName, name).one();
    }

    @Override
    public Page<StudentPo> getStudents(PageQuery query) {
        return Query.of(mapper).orderBy(StudentPo::getGmtCreate, false).page(query.getPage(), query.getLimit());
    }

    @Override
    public Pager<StudentPo> getStudentList(PageQuery query) {
        return Query.of(mapper).orderBy(StudentPo::getGmtCreate, false).pager(query.getPage(), query.getLimit());
    }

}
