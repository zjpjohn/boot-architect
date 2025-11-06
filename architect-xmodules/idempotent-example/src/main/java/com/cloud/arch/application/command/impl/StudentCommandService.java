package com.cloud.arch.application.command.impl;

import com.cloud.arch.application.assembler.StudentAssembler;
import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.idempotent.annotation.Idempotent;
import com.cloud.arch.infrast.persist.mapper.StudentMapper;
import com.cloud.arch.infrast.persist.po.StudentPo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCommandService implements IStudentCommandService {

    private final StudentMapper    mapper;
    private final StudentAssembler assembler;

    @Override
    @Transactional
    @Idempotent(prefix = "stu", key = "#cmd.name")
    public StudentPo createStudent(StudentCreateCmd cmd) {
        StudentPo student = assembler.toPo(cmd);
        mapper.insert(student);
        return student;
    }

}
