package com.boot.architect.adapter;

import com.boot.architect.application.command.IStudentCommandService;
import com.boot.architect.application.command.dto.StudentCreateCmd;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.page.PageQuery;
import com.cloud.arch.page.Pager;
import com.cloud.arch.web.annotation.ApiBody;
import com.mybatisflex.core.paginate.Page;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentCommandService commandService;

    @PostMapping
    public void create(@Validated @RequestBody StudentCreateCmd command) {
        commandService.create(command);
    }

    @GetMapping
    public StudentPo get(@NotNull(message = "唯一标识为空") Long id) {
        return commandService.getStudentById(id);
    }

    @GetMapping("/list")
    public Page<StudentPo> list(@Validated PageQuery query) {
        return commandService.getStudents(query);
    }

    @GetMapping("/page")
    public Pager<StudentPo> students(@Validated PageQuery query) {
        return commandService.getStudentList(query);
    }

}
