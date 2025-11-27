package com.boot.architect.adapter;

import com.boot.architect.application.command.IUserCommandService;
import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.application.command.dto.UserEditCmd;
import com.boot.architect.application.command.dto.UserListQuery;
import com.boot.architect.infrast.persist.po.UserPo;
import com.cloud.arch.page.Pager;
import com.cloud.arch.web.annotation.ApiBody;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@ApiBody
@Validated
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserCommandService userCommandService;

    @PostMapping
    public void create(@Validated UserCreateCmd command) {
        userCommandService.create(command);
    }

    @PutMapping
    public void edit(@Validated UserEditCmd command) {
        userCommandService.editUser(command);
    }

    @DeleteMapping
    public void remove(@NotNull(message = "唯一标识为空") Long id) {
        userCommandService.remove(id);
    }

    @GetMapping
    public UserPo user(@NotNull(message = "唯一标识为空") Long id) {
        return userCommandService.getUser(id);
    }

    @GetMapping("/list")
    public Pager<UserPo> userList(UserListQuery query) {
        return userCommandService.userList(query);
    }

}
