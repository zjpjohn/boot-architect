package com.boot.architect.application.command;

import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.application.command.dto.UserEditCmd;
import com.boot.architect.application.command.dto.UserListQuery;
import com.boot.architect.infrast.persist.po.UserPo;
import com.cloud.arch.page.Pager;

public interface IUserCommandService {

    void create(UserCreateCmd command);

    void editUser(UserEditCmd command);

    void remove(Long id);

    UserPo getUser(String name);

    UserPo getUser(Long id);

    Pager<UserPo> userList(UserListQuery query);

}
