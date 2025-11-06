package com.boot.architect.adapter;

import com.boot.architect.application.command.IStudentCommandService;
import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.application.command.dto.StudentEditCmd;
import com.cloud.arch.web.annotation.ApiBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/tx")
@RequiredArgsConstructor
public class AsyncTxController {

    private final IStudentCommandService commandService;

    @PostMapping("/")
    public void create(@Validated StudentCreateCmd cmd) {
        commandService.createStudent(cmd);
    }

    @PutMapping("/")
    public void edit(@Validated StudentEditCmd cmd) {
        commandService.editStudent(cmd);
    }

}
