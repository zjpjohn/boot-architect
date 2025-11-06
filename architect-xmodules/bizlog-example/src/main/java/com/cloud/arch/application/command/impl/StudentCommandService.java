package com.cloud.arch.application.command.impl;

import com.cloud.arch.annotation.RptCheck;
import com.cloud.arch.application.assembler.StudentAssembler;
import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.mapper.StudentMapper;
import com.cloud.arch.infrast.persist.po.StudentPo;
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

}
