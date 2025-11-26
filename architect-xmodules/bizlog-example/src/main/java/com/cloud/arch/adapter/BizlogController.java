package com.cloud.arch.adapter;

import com.cloud.arch.application.command.IStudentCommandService;
import com.cloud.arch.application.command.dto.StudentCreateCmd;
import com.cloud.arch.infrast.persist.po.StudentPo;
import com.cloud.arch.mybatis.extend.Pager;
import com.cloud.arch.page.Page;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.web.annotation.ApiBody;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping
    public StudentPo createStudent(@Validated StudentCreateCmd command) {
        return studentCommandService.createStudent(command);
    }

    @GetMapping
    public StudentPo student(@NotBlank(message = "学生名称为空") String name) {
        return studentCommandService.getStudent(name);
    }

    @GetMapping("/list")
    public Pager<StudentPo> students(@Validated PageQuery query) {
        return studentCommandService.getStudentList(query);
    }

    @GetMapping("/page")
    public Page<StudentPo> studentList(@Validated PageQuery query) {
        return studentCommandService.getStudents(query);
    }

}
