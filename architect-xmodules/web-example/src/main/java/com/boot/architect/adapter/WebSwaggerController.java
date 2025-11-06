package com.boot.architect.adapter;

import com.boot.architect.application.query.dto.Gender;
import com.boot.architect.application.query.dto.UserDto;
import com.cloud.arch.web.annotation.ApiBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@ApiBody
@Validated
@RestController
@Tag(name = "swagger接口文档")
@RequestMapping("/open")
public class WebSwaggerController {

    @GetMapping("/user")
    @Operation(summary = "查询用户")
    public UserDto user(UserDto user) {
        return user;
    }

    @GetMapping("/clazz")
    @Operation(summary = "班级查询用户集合")
    public List<UserDto> userByClazz(@NotBlank(message = "班级为空") String clazz) {
        return Collections.emptyList();
    }

    @GetMapping("/gender")
    @Operation(summary = "性别查询用户集合")
    public List<UserDto> userByGender(@NotNull(message = "性别为空") Gender gender) {
        return Collections.emptyList();
    }


}
