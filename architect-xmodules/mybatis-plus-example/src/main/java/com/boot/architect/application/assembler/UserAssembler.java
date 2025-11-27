package com.boot.architect.application.assembler;

import com.boot.architect.application.command.dto.UserCreateCmd;
import com.boot.architect.application.command.dto.UserEditCmd;
import com.boot.architect.infrast.persist.po.UserPo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserAssembler {

    UserPo toUser(UserCreateCmd cmd);

    UserPo toUser(UserEditCmd cmd);
}
