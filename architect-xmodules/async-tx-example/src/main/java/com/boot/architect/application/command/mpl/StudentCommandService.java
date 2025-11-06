package com.boot.architect.application.command.mpl;

import com.boot.architect.application.assembler.StudentAssembler;
import com.boot.architect.application.command.IStudentCommandService;
import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.application.command.dto.StudentEditCmd;
import com.boot.architect.application.command.executor.StudentCreateExecutor;
import com.boot.architect.application.command.executor.StudentEditExecutor;
import com.boot.architect.infrast.persist.mapper.StudentMapper;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.web.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCommandService implements IStudentCommandService {

    private final StudentMapper         mapper;
    private final StudentAssembler      assembler;
    private final StudentEditExecutor   editExecutor;
    private final StudentCreateExecutor createExecutor;

    @Override
    @Transactional
    public void createStudent(StudentCreateCmd cmd) {
        StudentPo student = assembler.toPo(cmd);
        mapper.insert(student);
        log.info("创建学生信息...");
        createExecutor.asyncAfter(student.getId(), student.getName());
    }

    @Override
    @Transactional
    public void editStudent(StudentEditCmd cmd) {
        StudentPo studentPo = mapper.selectById(cmd.getId());
        Assert.notNull(studentPo, "学生不存在");
        StudentPo student = assembler.toPo(cmd);
        mapper.updateById(student);
        log.info("编辑学生信息...");
        editExecutor.asyncAfter(student.getId(), student.getName());
        editExecutor.asyncNoArgs();
    }

}
