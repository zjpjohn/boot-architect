package com.cloud.arch.adapter;

import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.web.annotation.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class BizlogController {

    private final IStudentCommandService studentCommandService;

    @PostMapping("/")
    public StudentPo createStudent(@Validated StudentCreateCmd command) {
        return studentCommandService.createStudent(command);
    }

}
