package com.boot.architect.adapter;

import com.boot.architect.application.query.IStudentQueryService;
import com.boot.architect.infrast.persist.enums.Gender;
import com.boot.architect.infrast.persist.po.StudentPo;
import com.cloud.arch.enums.Value;
import com.cloud.arch.web.annotation.ApiBody;
import com.cloud.arch.web.validation.annotation.Enumerable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class CacheAppController {

    private final IStudentQueryService studentQueryService;

    @GetMapping("/{id}")
    public StudentPo student(@PathVariable Long id) {
        return studentQueryService.getStudent(id);
    }

    @GetMapping("/gender")
    public Gender studer(@NotNull(message = "性别错误") @Enumerable(ranges = "1", message = "性别表示错误")
                         Gender gender) {
        return gender;
    }

    @GetMapping("/gender1")
    public Gender studer1(@NotBlank(message = "性别错误") @Enumerable(ranges = "1", message = "性别标识错误")
                          Integer gender) {
        return Value.valueOf(gender, Gender.class);
    }

}
