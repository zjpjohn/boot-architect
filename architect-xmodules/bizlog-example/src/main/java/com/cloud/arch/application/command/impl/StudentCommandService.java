package com.cloud.arch.application.command.impl;

import com.alibaba.fastjson2.JSON;
import com.cloud.arch.annotation.RptCheck;
import com.cloud.arch.application.assembler.StudentAssembler;
import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.mapper.StudentMapper;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.mybatis.extend.LambdaQuery;
import com.cloud.arch.mybatis.extend.Page;
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

        StudentPo studentPo = LambdaQuery.<StudentPo>from().eq(StudentPo::getName, "张三").one(mapper::selectOne);
        log.info(JSON.toJSONString(studentPo));

        Page<StudentPo> page = LambdaQuery.from(StudentPo.class)
                                          .eq(StudentPo::getName, "张三")
                                          .page(Page.of(1, 5))
                                          .pageList(mapper::selectPage);
        log.info(JSON.toJSONString(page));

        StudentPo student = assembler.toPo(cmd);
        mapper.insert(student);
        return student;
    }

}
